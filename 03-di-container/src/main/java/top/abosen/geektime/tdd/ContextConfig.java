package top.abosen.geektime.tdd;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {
    public ContextConfig() {
        scope(Singleton.class, SingletonProvider::new);
    }

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<?>> getDependencies() {
            return List.of();
        }
    }

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();
    private Map<Class<?>, Function<ComponentProvider<?>, ComponentProvider<?>>> scopes = new HashMap<>();

    public <T> void scope(Class<T> scopeType, Function<ComponentProvider<?>, ComponentProvider<?>> provider) {
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
        if (Arrays.stream(annotations).map(Annotation::annotationType)
                .anyMatch(it -> !it.isAnnotationPresent(Qualifier.class) && !it.isAnnotationPresent(Scope.class))) {
            throw new IllegalComponentException();
        }

        Optional<Annotation> scopeFromType = Arrays.stream(implementation.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Scope.class)).findFirst();

        List<Annotation> qualifiers = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        Optional<Annotation> scope = Arrays.stream(annotations).filter(a -> a.annotationType().isAnnotationPresent(Scope.class))
                .findFirst().or(() -> scopeFromType);

        ComponentProvider<R> injectionProvider = new InjectionProvider<>(implementation);
        ComponentProvider<R> provider = scope.map(s -> getScopeProvider(s, injectionProvider)).orElse(injectionProvider);

        if (qualifiers.isEmpty()) {
            components.put(new Component(type, null), provider);
        }

        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), provider);
        }
    }

    private  <T> ComponentProvider<T> getScopeProvider(Annotation annotation, ComponentProvider<T> provider) {
        return (ComponentProvider<T>) scopes.get(annotation.annotationType()).apply(provider);
    }

    static class SingletonProvider<T> implements ComponentProvider<T> {
        private T singleton;
        private ComponentProvider<T> provider;

        public SingletonProvider(ComponentProvider<T> provider) {
            this.provider = provider;
        }

        @Override
        public T get(Context context) {
            if (singleton == null) {
                singleton = provider.get(context);
            }
            return singleton;
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
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
                return (T) getOpt(ref).orElseThrow(() -> new DependencyNotFountException(ref.component(), ref.component()));
            }
        };
    }

    private <T> ComponentProvider<?> getProvider(ComponentRef<T> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Deque<Component> visiting) {
        for (ComponentRef<?> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFountException(dependency.component(), component);
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.component())) {
                    List<Component> cyclicPath = new ArrayList<>(visiting);
                    cyclicPath.add(dependency.component());
                    throw new CyclicDependenciesFoundException(cyclicPath);
                }
                visiting.addLast(dependency.component());

                checkDependencies(dependency.component(), visiting);
                visiting.removeLast();
            }
        }
    }


}
