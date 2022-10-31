package top.abosen.geektime.tdd;

import jakarta.inject.Provider;

import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref> getDependencies() {
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
            public <T> Optional<T> getOpt(Ref ref) {
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(providers.get(ref.getComponent())).map(it -> (T) (Provider<Object>) () -> it.get(this));
                }
                return (Optional<T>) Optional.ofNullable(providers.get(ref.getComponent())).map(it -> (Object) it.get(this));
            }

            @Override
            public <T> T get(Ref ref) {
                return (T) getOpt(ref).orElseThrow(() -> new DependencyNotFountException(ref.getComponent(), ref.getComponent()));
            }
        };
    }

    private void checkDependencies(Class<?> component, Deque<Class<?>> visiting) {
        for (Context.Ref dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency.getComponent())) {
                throw new DependencyNotFountException(dependency.getComponent(), component);
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) {
                    List<Class<?>> cyclicPath = new ArrayList<>(visiting);
                    cyclicPath.add(dependency.getComponent());
                    throw new CyclicDependenciesFoundException(cyclicPath);
                }
                visiting.addLast(dependency.getComponent());
                checkDependencies(dependency.getComponent(), visiting);
                visiting.removeLast();
            } else {
                // todo transitive

            }
        }
    }


}
