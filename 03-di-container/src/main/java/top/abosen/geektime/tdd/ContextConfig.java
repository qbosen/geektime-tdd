package top.abosen.geektime.tdd;

import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Class<?>> getDependencies() {
            return List.of();
        }
    }

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (ComponentProvider<Type>) context -> instance);
    }

    public <Type, Implementation extends Type> void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new InjectionProvider<>(implementation));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new ArrayDeque<>()));
        return new Context() {
            @Override
            public <Type> Type get(Class<Type> type) {
                return getOpt(type).orElseThrow(() -> new DependencyNotFountException(type, type));
            }

            @Override
            public <Type> Optional<Type> getOpt(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Deque<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
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

}
