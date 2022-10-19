package top.abosen.geektime.tdd;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class DependencyNotFountException extends RuntimeException{
    private Class<?> dependency;
    private Class<?> component;

    public DependencyNotFountException(Class<?> dependency, Class<?> component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Class<?> getDependency() {
        return dependency;
    }

    public Class<?> getComponent() {
        return component;
    }

}
