package top.abosen.geektime.tdd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public Class<?>[] getComponents() {
        return components.toArray(Class[]::new);
    }
}
