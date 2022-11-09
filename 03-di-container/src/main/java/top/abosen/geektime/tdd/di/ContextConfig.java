package top.abosen.geektime.tdd.di;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public <T> void scope(Class<T> scopeType, ScopeProvider provider) {
        scopes.put(scopeType, provider);
    }

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation) {
        bind(type, implementation, implementation.getAnnotations());
    }

    public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(it -> !it.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), (ComponentProvider<T>) context -> instance);
        }
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation, Annotation... annotations) {

        Map<Class<?>, List<Annotation>> annotationGroups = Arrays.stream(annotations).collect(Collectors.groupingBy(this::typeOf, Collectors.toList()));

        if (annotationGroups.containsKey(Illegal.class)) {
            throw new IllegalComponentException();
        }

        bind(type, annotationGroups.getOrDefault(Qualifier.class, List.of()),
                createScopeProvider(implementation, annotationGroups.getOrDefault(Scope.class, List.of())));
    }

    private <T, R extends T> ComponentProvider<R> createScopeProvider(Class<R> implementation, List<Annotation> scopes) {
        if (scopes.size() > 1) throw new IllegalComponentException();
        ComponentProvider<R> injectionProvider = new InjectionProvider<>(implementation);
        return scopes.stream().findFirst().or(() -> scopeFromImplementation(implementation))
                .map(s -> getScopeProvider(s, injectionProvider)).orElse(injectionProvider);
    }

    private <T> void bind(Class<T> type, List<Annotation> qualifiers, ComponentProvider<?> provider) {
        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private static Optional<Annotation> scopeFromImplementation(Class<?> implementation) {
        return Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();
    }

    private @interface Illegal {
    }

    private Class<?> typeOf(Annotation annotation) {
        return Stream.of(Qualifier.class, Scope.class).filter(annotation.annotationType()::isAnnotationPresent).findFirst().orElse(Illegal.class);
    }

    private <T> ComponentProvider<T> getScopeProvider(Annotation scope, ComponentProvider<T> provider) {
        if (!scopes.containsKey(scope.annotationType())) throw new IllegalComponentException();
        return (ComponentProvider<T>) scopes.get(scope.annotationType()).create(provider);
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
