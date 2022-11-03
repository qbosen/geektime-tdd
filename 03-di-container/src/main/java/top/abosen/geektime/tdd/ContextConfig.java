package top.abosen.geektime.tdd;

import jakarta.inject.Provider;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        default List<Context.Ref<Object>> getDependencies() {
            return List.of();
        }
    }

    private Map<Component, ComponentProvider<?>> components = new HashMap<>();

    public <T> void bind(Class<T> type, T instance) {
        components.put(new Component(type, null), (ComponentProvider<T>) context -> instance);
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation) {
        components.put(new Component(type, null), new InjectionProvider<>(implementation));
    }

    public <T> void bind(Class<T> type, T instance, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), (ComponentProvider<T>) context -> instance);
        }
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation, Annotation... qualifiers) {
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
        }
    }

    record Component(Class<?> type, Annotation qualifier) {
    }


    public Context getContext() {
        components.keySet().forEach(component -> checkDependencies(component, new ArrayDeque<>()));

        return new Context() {
            @Override
            public <T> Optional<T> getOpt(Ref<T> ref) {
                if (ref.getQualifier() != null) {
                    return (Optional<T>) Optional.ofNullable(components.get(new Component(ref.getComponent(), ref.getQualifier())))
                            .map(it -> (Object) it.get(this));
                }
                if (ref.isContainer()) {
                    if (ref.getContainer() != Provider.class) return Optional.empty();
                    return Optional.ofNullable(getProvider(ref)).map(it -> (T) (Provider<Object>) () -> it.get(this));
                }
                return (Optional<T>) Optional.ofNullable(getProvider(ref)).map(it -> (Object) it.get(this));
            }

            @Override
            public <T> T get(Ref<T> ref) {
                return (T) getOpt(ref).orElseThrow(() -> new DependencyNotFountException(ref.getComponent(), ref.getComponent()));
            }
        };
    }

    private <T> ComponentProvider<?> getProvider(Context.Ref<T> ref) {
        return components.get(new Component(ref.getComponent(), ref.getQualifier()));
    }

    private void checkDependencies(Component  component, Deque<Class<?>> visiting) {
        for (Context.Ref<Object> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(new Component(dependency.getComponent(), dependency.getQualifier()))) {
                throw new DependencyNotFountException(dependency.getComponent(), component.type());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponent())) {
                    List<Class<?>> cyclicPath = new ArrayList<>(visiting);
                    cyclicPath.add(dependency.getComponent());
                    throw new CyclicDependenciesFoundException(cyclicPath);
                }
                visiting.addLast(dependency.getComponent());

                checkDependencies(new Component(dependency.getComponent(), dependency.getQualifier()), visiting);
                visiting.removeLast();
            } else {
                // todo transitive

            }
        }
    }


}
