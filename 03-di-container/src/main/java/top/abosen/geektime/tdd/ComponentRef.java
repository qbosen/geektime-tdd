package top.abosen.geektime.tdd;

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
        return new ComponentRef<>(component);
    }

    public static <T> ComponentRef<T> of(Type type, Annotation qualifier) {
        return new ComponentRef<>(type, qualifier);
    }

    public static <T> ComponentRef<T> of(Type type) {
        return new ComponentRef<>(type, null);
    }

    ComponentRef(Type container, Annotation qualifier) {
        init(container, qualifier);
    }

    ComponentRef(Class<T> componentType) {
        init(componentType, null);
    }

    protected ComponentRef() {
        init(((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0], null);
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

    public Class<?> getComponentType() {
        return component.type();
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
