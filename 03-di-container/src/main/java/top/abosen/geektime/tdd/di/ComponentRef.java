package top.abosen.geektime.tdd.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author qiubaisen
 * @date 2022/10/31
 */
public class ComponentRef<T> {
    private Type container;
    private Component component;

    public static <T> ComponentRef<T> of(Class<T> component) {
        return new ComponentRef<>(component, null);
    }

    public static <T> ComponentRef<T> of(Type type) {
        return new ComponentRef<>(type, null);
    }

    public static <T> ComponentRef<T> of(Type type, Annotation qualifier) {
        return new ComponentRef<>(type, qualifier);
    }

    private ComponentRef(Type container, Annotation qualifier) {
        init(container, qualifier);
    }

    protected ComponentRef() {
        this(null);
    }

    protected ComponentRef(Annotation qualifier) {
        init(((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0], qualifier);
    }

    private void init(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType container) {
            this.container = container.getRawType();
            this.component = new Component((Class<T>) container.getActualTypeArguments()[0], qualifier);

        } else {
            this.component = new Component((Class<T>) type, qualifier);
        }
    }


    public Type getContainer() {
        return container;
    }

    public boolean isContainer() {
        return container != null;
    }

    public Component component() {
        return component;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentRef<?> that = (ComponentRef<?>) o;
        return Objects.equals(container, that.container) && component.equals(that.component);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, component);
    }
}
