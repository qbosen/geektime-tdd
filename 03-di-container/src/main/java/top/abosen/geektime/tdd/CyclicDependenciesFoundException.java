package top.abosen.geektime.tdd;

import java.util.ArrayList;
import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private final List<Class<?>> components;

    public CyclicDependenciesFoundException(Class<?> dependency, List<Class<?>> visiting) {
        components = new ArrayList<>(visiting);
        components.add(dependency);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class[]::new);
    }
}
