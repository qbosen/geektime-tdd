package top.abosen.geektime.tdd.di;

import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author qiubaisen
 * @date 2022/11/14
 */
public class Utils {
    interface TestComponent {
        default Dependency dependency() {
            return null;
        }
    }

    interface Dependency {
    }

    interface AnotherDependency {
    }

    static record NamedLiteral(String value) implements jakarta.inject.Named {
        @Override
        public Class<? extends Annotation> annotationType() {
            return jakarta.inject.Named.class;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof jakarta.inject.Named named && Objects.equals(this.value, named.value());
        }

        @Override
        public int hashCode() {
            return "value".hashCode() * 127 ^ value.hashCode();
        }
    }

    @Qualifier
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Skywalker {
    }

    @Scope
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @interface Pooled {
    }

    record PooledLiteral() implements Pooled {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Pooled.class;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Pooled;
        }
    }

    static class PooledProvider<T> implements ComponentProvider<T> {
        static int MAX = 2;
        int current;
        private List<T> pool;
        private ComponentProvider<T> provider;

        public PooledProvider(ComponentProvider<T> provider) {
            this.provider = provider;
            this.pool = new ArrayList<>();
            this.current = 0;
        }

        @Override
        public T get(Context context) {
            if (current < MAX) {
                pool.add(provider.get(context));
            }

            return pool.get(current++ % MAX);
        }

        @Override
        public List<ComponentRef<?>> getDependencies() {
            return provider.getDependencies();
        }
    }

    record SingletonLiteral() implements Singleton {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Singleton.class;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Singleton;
        }
    }

    record SkywalkerLiteral() implements Skywalker {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Skywalker.class;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Skywalker;
        }
    }

    record TestLiteral() implements Test {
        @Override
        public Class<? extends Annotation> annotationType() {
            return Test.class;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Test;
        }
    }
}
