package top.abosen.geektime.tdd;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class DependencyNotFountException extends RuntimeException{
    private Component dependency;
    private Component component;

    public DependencyNotFountException(Component dependency, Component component) {
        this.dependency = dependency;
        this.component = component;
    }

    public Component getDependency() {
        return dependency;
    }

    public Component getComponent() {
        return component;
    }
}
