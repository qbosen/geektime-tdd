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
            public <T> T get(Class<T> type) {
                return getOpt(type).orElseThrow(() -> new DependencyNotFountException(type, type));
            }

            @Override
            public <T> Optional<T> getOpt(Class<T> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (T) it.get(this));
            }

            @Override
            public <T> Provider<T> get(ParameterizedType type) {
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                return (Provider<T>) getOpt(type).orElseThrow(() -> new DependencyNotFountException(componentType, componentType));
            }

            @Override
            public <T> Optional<Provider<T>> getOpt(ParameterizedType type) {
                if (type.getRawType() != Provider.class) return Optional.empty();
                Class<?> componentType = (Class<?>) type.getActualTypeArguments()[0];
                return Optional.ofNullable(providers.get(componentType)).map(it -> (Provider<T>) () -> (T) it.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Deque<Class<?>> visiting) {
        for (Type dependency : providers.get(component).getDependencies()) {
            if (dependency instanceof Class<?>) {
                checkDependency(component, visiting, (Class<?>) dependency);
            }
            if (dependency instanceof ParameterizedType) {
                Class<?> type = (Class<?>) ((ParameterizedType) dependency).getActualTypeArguments()[0];
                // todo transitive
                if (!providers.containsKey(type)) {
                    throw new DependencyNotFountException(type, component);
                }
            }
        }
    }

    private void checkDependency(Class<?> component, Deque<Class<?>> visiting, Class<?> dependency) {
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
