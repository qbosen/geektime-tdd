package top.abosen.geektime.tdd;

import jakarta.inject.Provider;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        default List<ComponentRef<Object>> getDependencies() {
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
        if (Arrays.stream(qualifiers).anyMatch(it->!it.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), (ComponentProvider<T>) context -> instance);
        }
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implementation, Annotation... qualifiers) {
        if (Arrays.stream(qualifiers).anyMatch(it->!it.annotationType().isAnnotationPresent(Qualifier.class))) {
            throw new IllegalComponentException();
        }
        for (Annotation qualifier : qualifiers) {
            components.put(new Component(type, qualifier), new InjectionProvider<>(implementation));
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
                return (T) getOpt(ref).orElseThrow(() -> new DependencyNotFountException(ref.getComponentType(), ref.getComponentType()));
            }
        };
    }

    private <T> ComponentProvider<?> getProvider(ComponentRef<T> ref) {
        return components.get(ref.component());
    }

    private void checkDependencies(Component component, Deque<Class<?>> visiting) {
        for (ComponentRef<Object> dependency : components.get(component).getDependencies()) {
            if (!components.containsKey(dependency.component())) {
                throw new DependencyNotFountException(dependency.getComponentType(), component.type());
            }
            if (!dependency.isContainer()) {
                if (visiting.contains(dependency.getComponentType())) {
                    List<Class<?>> cyclicPath = new ArrayList<>(visiting);
                    cyclicPath.add(dependency.getComponentType());
                    throw new CyclicDependenciesFoundException(cyclicPath);
                }
                visiting.addLast(dependency.getComponentType());

                checkDependencies(dependency.component(), visiting);
                visiting.removeLast();
            } else {
                // todo transitive

            }
        }
    }


}
