package top.abosen.geektime.tdd;

import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private final List<Class<?>> components;

    public CyclicDependenciesFoundException(List<Class<?>> components) {
        this.components = components;
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class[]::new);
    }
}
