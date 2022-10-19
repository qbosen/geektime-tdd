package top.abosen.geektime.tdd;

import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private List<Class<?>> components = new ArrayList<>();

    public CyclicDependenciesFoundException(Class<?> componentType) {
        components.add(componentType);
    }

    public CyclicDependenciesFoundException(Class<?> componentType, CyclicDependenciesFoundException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public CyclicDependenciesFoundException(Class<?> dependency, List<Class<?>> visiting) {
        components = new ArrayList<>(visiting);
        components.add(dependency);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class[]::new);
    }
}
