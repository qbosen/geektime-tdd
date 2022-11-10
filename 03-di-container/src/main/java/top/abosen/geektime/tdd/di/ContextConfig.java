package top.abosen.geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {
    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    static class Bindings {
        private @interface Illegal {
        }

        private final Class<?> type;
        private final Map<Class<?>, List<Annotation>> group;

        public Bindings(Class<?> type, Annotation... annotations) {
            this.type = type;
            this.group = parse(annotations);
        }

        public List<Annotation> qualifiers() {
            return group.getOrDefault(Qualifier.class, List.of());
        }

        Optional<Annotation> scope() {
            List<Annotation> scopes = group.getOrDefault(Scope.class, scopeFrom(type));
            if (scopes.size() > 1) {
                throw new IllegalComponentException();
            }
            return scopes.stream().findFirst();
        }

        private ComponentProvider<?> provider(BiFunction<ComponentProvider<?>, Annotation, ComponentProvider<?>> scoped) {
            ComponentProvider<?> injectionProvider = new InjectionProvider<>(type);
            return scope().<ComponentProvider<?>>map(s -> scoped.apply(injectionProvider, s)).orElse(injectionProvider);
        }

        private static List<Annotation> scopeFrom(Class<?> implementation) {
            return stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).toList();
        }

        private static Map<Class<?>, List<Annotation>> parse(Annotation[] annotations) {
            Map<Class<?>, List<Annotation>> annotationGroup = stream(annotations).collect(Collectors.groupingBy(Bindings::typeOf, Collectors.toList()));
            if (annotationGroup.containsKey(Illegal.class)) {
                throw new IllegalComponentException();
            }
            return annotationGroup;
        }

        private static Class<?> typeOf(Annotation annotation) {
            return Stream.of(Qualifier.class, Scope.class).filter(annotation.annotationType()::isAnnotationPresent).findFirst().orElse(Illegal.class);
        }
    }

    public <T> void scope(Class<T> scopeType, ScopeProvider provider) {
        scopes.put(scopeType, provider);
    }

    public <T, R extends T> void bindComponent(Class<T> type, Class<R> implementation, Annotation... annotations) {
        Annotation[] componentAnnotations = Stream.concat(stream(implementation.getAnnotations()), stream(annotations)).toArray(Annotation[]::new);
        Bindings bindings = new Bindings(implementation, componentAnnotations);
        bind(type, bindings.qualifiers(), bindings.provider(this::scopeProvider));
    }

    public <T> void bindInstance(Class<T> type, T instance, Annotation... annotations) {
        Bindings bindings = new Bindings(type, annotations);
        bind(type, bindings.qualifiers(), context -> instance);
    }


    private <T> void bind(Class<T> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }


    private ComponentProvider<?> scopeProvider(ComponentProvider<?> provider, Annotation scope) {
        if (!scopes.containsKey(scope.annotationType())) throw new IllegalComponentException();
        return scopes.get(scope.annotationType()).create(provider);
    }

    public void from(Config config) {
        new DSL(config).bind();
    }

    private class DSL {

        private final Config config;

        DSL(Config config) {
            this.config = config;
        }

        void bind() {
            declarations().forEach(declaration -> declaration.value().ifPresentOrElse(declaration::bindInstance, declaration::bindComponent));
        }

        private List<Declaration> declarations() {
            return stream(config.getClass().getDeclaredFields()).filter(it -> !it.isSynthetic()).map(Declaration::new).toList();
        }

        private class Declaration {
            private final Field field;

            Declaration(Field field) {
                this.field = field;
            }

            void bindInstance(Object instance) {
                ContextConfig.this.bindInstance((Class<Object>) type(), instance, annotations());
            }

            void bindComponent() {
                ContextConfig.this.bindComponent((Class<Object>) type(), (Class<Object>) field.getType(), annotations());
            }

            private Optional<Object> value() {
                try {
                    return Optional.ofNullable(field.get(config));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            private Class<?> type() {
                Config.Export export = field.getAnnotation(Config.Export.class);
                return export == null ? field.getType() : export.value();
            }

            private Annotation[] annotations() {
                return stream(field.getAnnotations()).filter(it -> !isConfigAnnotation(it)).toArray(Annotation[]::new);
            }

            private boolean isConfigAnnotation(Annotation it) {
                return it.annotationType().getEnclosingClass() == Config.class;
            }
        }

    }


    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new ArrayDeque<>()));

        return new Context() {
            @Override
            public <T> Optional<T> getOpt(ComponentRef<T> ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(getProvider(ref)).map(it -> (T) (Provider<Object>) () -> it.get(this));
                }
                return (Optional<T>) Optional.ofNullable(getProvider(ref)).map(it -> (Object) it.get(this));
            }

            @Override
            public <T> T get(ComponentRef<T> ref) {
                return getOpt(ref).orElseThrow(() -> ContextConfigError.unsatisfiedResolution(ref.component(), ref.component()));
            }
        };
    }

    private <T> ComponentProvider<?> getProvider(ComponentRef<T> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Deque<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw ContextConfigError.unsatisfiedResolution(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    List<Component> cyclicPath = new ArrayList<>(visiting);
                    throw ContextConfigError.circularDependencies(cyclicPath, component);
                }
                visiting.addLast(dependency.component());

                checkDependencies(dependency.component(), visiting);
                visiting.removeLast();
            }
        }
    }
}

class ContextConfigError extends Error {
    public static ContextConfigError unsatisfiedResolution(Component component, Component dependency) {
        return new DependencyNotFount(component, dependency);
    }

    public static ContextConfigError circularDependencies(Collection<Component> path, Component circular) {
        return new CyclicDependenciesFound(path, circular);
    }

    ContextConfigError(String message) {
        super(message);
    }

    static class DependencyNotFount extends ContextConfigError {
        private final Component dependency;
        private final Component component;

        public DependencyNotFount(Component component, Component dependency) {
            super(MessageFormat.format("Unsatisfied resolution: {1} for {0}", component, dependency));
            this.dependency = dependency;
            this.component = component;
        }

        public Component dependency() {
            return dependency;
        }

        public Component component() {
            return component;
        }
    }

    static class CyclicDependenciesFound extends ContextConfigError {

        private final Collection<Component> path;
        private final Component circular;

        public CyclicDependenciesFound(Collection<Component> path, Component circular) {
            super(MessageFormat.format("Circular dependencies: {0} -> [{1}]",
                    path.stream().map(Objects::toString).collect(joining(" -> ")), circular));
            this.path = path;
            this.circular = circular;
        }

        public Class<?>[] getPath() {
            return Stream.concat(path.stream(), Stream.of(circular)).map(Component::type).toArray(Class[]::new);
        }
    }
}
