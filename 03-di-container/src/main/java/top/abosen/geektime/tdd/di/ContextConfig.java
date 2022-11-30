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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.*;
import static top.abosen.geektime.tdd.di.ContextConfigError.circularDependencies;
import static top.abosen.geektime.tdd.di.ContextConfigError.unsatisfiedResolution;
import static top.abosen.geektime.tdd.di.ContextConfigException.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {
    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    private final Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private final Map<Class<?>, ScopeProvider> scopes = new HashMap<>();

    static class Bindings {
        public static Bindings component(Class<?> component, Annotation... annotations) {
            return new Bindings(component, annotations, Qualifier.class, Scope.class);
        }

        public static Bindings instance(Class<?> instance, Annotation... annotations) {
            return new Bindings(instance, annotations, Qualifier.class);
        }

        private @interface Illegal {
        }

        private final Class<?> type;
        private final Map<Class<?>, List<Annotation>> group;

        public Bindings(Class<?> type, Annotation[] annotations, Class<? extends Annotation>... allowed) {
            this.type = type;
            this.group = parse(type, annotations, allowed);
        }

        public List<Annotation> qualifiers() {
            return group.getOrDefault(Qualifier.class, List.of());
        }

        Optional<Annotation> scope() {
            List<Annotation> scopes = group.getOrDefault(Scope.class, from(type, Scope.class));
            if (scopes.size() > 1) {
                throw illegalAnnotation(type, scopes);
            }
            return scopes.stream().findFirst();
        }

        private ComponentProvider<?> provider(BiFunction<ComponentProvider<?>, Annotation, ComponentProvider<?>> scoped) {
            ComponentProvider<?> injectionProvider = new InjectionProvider<>(type);
            return scope().<ComponentProvider<?>>map(s -> scoped.apply(injectionProvider, s)).orElse(injectionProvider);
        }

        private static List<Annotation> from(Class<?> implementation, Class<? extends Annotation> annotation) {
            return stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(annotation)).toList();
        }

        private Map<Class<?>, List<Annotation>> parse(Class<?> type, Annotation[] annotations, Class<? extends Annotation>... allowed) {
            Map<Class<?>, List<Annotation>> annotationGroup = stream(annotations).collect(groupingBy(allow(allowed), toList()));
            if (annotationGroup.containsKey(Illegal.class)) {
                throw illegalAnnotation(type, annotationGroup.get(Illegal.class));
            }
            return annotationGroup;
        }

        private static Function<Annotation, Class<?>> allow(Class<? extends Annotation>... allowed) {
            return annotation -> Stream.of(allowed).filter(annotation.annotationType()::isAnnotationPresent).findFirst().orElse(Illegal.class);
        }
    }

    public <T> void scope(Class<T> scopeType, ScopeProvider provider) {
        scopes.put(scopeType, provider);
    }

    public <T, R extends T> void bindComponent(Class<T> type, Class<R> implementation, Annotation... annotations) {
        Bindings bindings = Bindings.component(implementation, annotations);
        bind(type, bindings.qualifiers(), bindings.provider(this::scopeProvider));
    }

    public <T> void bindInstance(Class<T> type, T instance, Annotation... annotations) {
        Bindings bindings = Bindings.instance(type, annotations);
        bind(type, bindings.qualifiers(), context -> instance);
    }

    private <T> void bind(Class<T> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) bind(new Component(type, null), provider);
        for (Annotation qualifier : qualifiers) bind(new Component(type, qualifier), provider);
    }

    private ComponentProvider<?> bind(Component component, ComponentProvider<?> provider) {
        if (components.containsKey(component)) throw duplicated(component);
        return components.put(component, provider);
    }


    private ComponentProvider<?> scopeProvider(ComponentProvider<?> provider, Annotation scope) {
        if (!scopes.containsKey(scope.annotationType())) throw unknownScope(scope.annotationType());
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
                    field.setAccessible(true);
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
                return getOpt(ref).orElseThrow(() -> unsatisfiedResolution(ref.component(), ref.component()));
            }
        };
    }

    private <T> ComponentProvider<?> getProvider(ComponentRef<T> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Deque<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw unsatisfiedResolution(component, dependency.component());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    List<Component> cyclicPath = new ArrayList<>(visiting);
                    throw circularDependencies(cyclicPath, component);
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

class ContextConfigException extends RuntimeException {
    static ContextConfigException illegalAnnotation(Class<?> type, List<Annotation> annotations) {
        return new ContextConfigException(MessageFormat.format("Unqualified annotations: {0} of {1}",
                String.join(" , ", annotations.stream().map(Objects::toString).toList()), type));
    }

    static ContextConfigException unknownScope(Class<? extends Annotation> annotationType) {
        return new ContextConfigException(MessageFormat.format("Unknown scope: {0}", annotationType));
    }

    static ContextConfigException duplicated(Component component) {
        return new ContextConfigException(MessageFormat.format("Duplicated: {0}", component));
    }

    ContextConfigException(String message) {
        super(message);
    }
}