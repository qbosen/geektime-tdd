package top.abosen.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Type> getDependencies() {
            return List.of();
        }
    }

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        providers.put(type, (ComponentProvider<T>) context -> instance);
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new ArrayDeque<>()));
        return new Context() {
            @Override
            public <T> Optional<T> getOpt(Type type) {
                if (isContainerType(type)) {
                    return (Optional<T>) getContainerOpt((ParameterizedType) type);
                }
                return (Optional<T>) getComponentOpt((Class<?>) type);
            }

            @Override
            public <T> T get(Type type) {
                if (isContainerType(type)) {
                    return (T) getContainer((ParameterizedType) type);
                }
                return (T) getComponent((Class<?>) type);
            }

            private <T> T getComponent(Class<T> type) {
                return getComponentOpt(type).orElseThrow(() -> new DependencyNotFountException(type, type));
            }

            private <T> Optional<T> getComponentOpt(Class<T> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (T) it.get(this));
            }

            private <T> Provider<T> getContainer(ParameterizedType type) {
                Class<?> componentType = getComponentType(type);
                return (Provider<T>) getContainerOpt(type).orElseThrow(() -> new DependencyNotFountException(componentType, componentType));
            }

            private <T> Optional<Provider<T>> getContainerOpt(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<?> componentType = getComponentType(type);
                return Optional.ofNullable(providers.get(componentType)).map(it -> (Provider<T>) () -> (T) it.get(this));
            }
        };
    }

    private static Class<?> getComponentType(Type type) {
        return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
    }

    private static boolean isContainerType(Type type) {
        return type instanceof ParameterizedType;
    }

    private void checkDependencies(Class<?> component, Deque<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (isContainerType(dependency)) checkContainerDependency(component, dependency);
            else checkComponentDependency(component, visiting, (Class<?>) dependency);
        }
    }

    private void checkContainerDependency(Class<?> component, Type dependency) {
        Class<?> type = getComponentType(dependency);
        // todo transitive
        if (!providers.containsKey(type)) {
            throw new DependencyNotFountException(type, component);
        }
    }

    private void checkComponentDependency(Class<?> component, Deque<Class<?>> visiting, Class<?> dependency) {
        if (!providers.containsKey(dependency)) {
            throw new DependencyNotFountException(dependency, component);
        }
        if (visiting.contains(dependency)) {
            List<Class<?>> cyclicPath = new ArrayList<>(visiting);
            cyclicPath.add(dependency);
            throw new CyclicDependenciesFoundException(cyclicPath);
        }
        visiting.addLast(dependency);
        checkDependencies(dependency, visiting);
        visiting.removeLast();
    }

}
