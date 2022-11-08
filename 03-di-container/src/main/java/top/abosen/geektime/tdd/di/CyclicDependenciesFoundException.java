package top.abosen.geektime.tdd.di;

import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class CyclicDependenciesFoundException extends RuntimeException {

    private final List<Component> components;

    public CyclicDependenciesFoundException(List<Component> components) {
        this.components = components;
    }

    public Class<?>[] getComponents() {
        return components.stream().map(Component::type).toArray(Class[]::new);
    }
}
