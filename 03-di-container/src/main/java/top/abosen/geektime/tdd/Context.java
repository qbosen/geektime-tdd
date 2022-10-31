package top.abosen.geektime.tdd;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
public interface Context {
    <T> T get(Ref ref);

    <T> Optional<T> getOpt(Ref ref);

    /**
     * @author qiubaisen
     * @date 2022/10/31
     */
    class Ref {
        private Type container;
        private Class<?> component;

        public static Ref of(Type type) {
            if (type instanceof ParameterizedType container) {
                return new Ref(container);
            }
            return new Ref((Class<?>) type);
        }

        Ref(ParameterizedType container) {
            this.container = container.getRawType();
            this.component = (Class<?>) container.getActualTypeArguments()[0];
        }

        Ref(Class<?> component) {
            this.component = component;
        }

        public Type getContainer() {
            return container;
        }

        public Class<?> getComponent() {
            return component;
        }

        public boolean isContainer() {
            return container != null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ref ref = (Ref) o;
            return Objects.equals(container, ref.container) && component.equals(ref.component);
        }

        @Override
        public int hashCode() {
            return Objects.hash(container, component);
        }
    }
}
