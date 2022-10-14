package top.abosen.geektime.tdd;

import java.util.HashMap;
import java.util.Map;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();
    private Map<Class<?>, Class<?>> componentImplementaions = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> type, ComponentType instance) {
        components.put(type, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        if (components.containsKey(type)) {
            return (ComponentType) components.get(type);
        }
        Class<?> implementation = componentImplementaions.get(type);
        try {
            return (ComponentType) implementation.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> type, Class<ComponentImplementation> implementation) {
        componentImplementaions.put(type, implementation);
    }
}
