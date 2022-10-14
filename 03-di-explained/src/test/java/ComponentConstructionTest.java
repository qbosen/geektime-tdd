import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/10/13
 */
public class ComponentConstructionTest {
    interface Engine {
        String getName();
    }

    interface Car {
        Engine getEngine();
    }

    static class V6Engine implements Engine {
        @Override
        public String getName() {
            return "V6";
        }
    }

    static class V8Engine implements Engine {
        @Override
        public String getName() {
            return "V8";
        }
    }


    @Nested
    public class DependencyInjection {
        static class CarInjectConstructor implements Car {
            private Engine engine;

            @Inject
            public CarInjectConstructor(Engine engine) {
                this.engine = engine;
            }

            @Override
            public Engine getEngine() {
                return engine;
            }
        }

        @Test
        public void constructor_injection() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Engine.class).to(V8Engine.class);
                    bind(Car.class).to(CarInjectConstructor.class);
                }
            });

            Car car = injector.getInstance(Car.class);
            assertEquals("V8", car.getEngine().getName());
        }


        static class CarInjectField implements Car {
            @Inject
            private Engine engine;

            @Override
            public Engine getEngine() {
                return engine;
            }
        }

        @Test
        public void field_injection() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Car.class).to(CarInjectField.class);
                    bind(Engine.class).to(V8Engine.class);
                }
            });

            Car car = injector.getInstance(Car.class);
            assertEquals("V8", car.getEngine().getName());
        }

        static class CarInjectMethod implements Car {
            private Engine engine;

            @Override
            public Engine getEngine() {
                return engine;
            }

            @Inject
            private void install(Engine engine) {
                this.engine = engine;
            }
        }

        @Test
        public void method_injection() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Car.class).to(CarInjectMethod.class);
                    bind(Engine.class).to(V8Engine.class);
                }
            });

            Car car = injector.getInstance(Car.class);
            assertEquals("V8", car.getEngine().getName());
        }
    }


    @Nested
    public class DependencySelection {
        record A(B b) {
            @Inject
            A {
            }
        }

        record B(A a) {
            @Inject
            B {
            }
        }

        @Test
        public void cyclic_dependencies() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(A.class);
                    bind(B.class);
                }
            });
            ProvisionException exception = assertThrows(ProvisionException.class, () -> {
                A a = injector.getInstance(A.class);
            });
            assertTrue(exception.getMessage().contains("circular dependency"));
        }

        record X(Y y) {
            @Inject
            X {
            }
        }

        static final class Y {
            private final Provider<X> x;

            @Inject
            Y(Provider<X> x) {
                this.x = x;
            }

            public X x() {
                return x.get();
            }
        }

        @Test
        public void cyclic_dependencies_with_provider() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(X.class);
                    bind(Y.class);
                }
            });
            X x = injector.getInstance(X.class);
            assertNotNull(x);
            assertNotNull(x.y());
            Y y = injector.getInstance(Y.class);
            assertNotNull(y);
            assertNotNull(y.x());
        }


        static class V8Car implements Car {
            @Inject
            @Named("V8")
            private Engine engine;

            @Override
            public Engine getEngine() {
                return engine;
            }
        }

        @Test
        public void selection_named() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Engine.class).annotatedWith(Names.named("V8")).to(V8Engine.class);
                    bind(Engine.class).annotatedWith(Names.named("V6")).to(V6Engine.class);
                    bind(Car.class).to(V8Car.class);
                }
            });

            Car car = injector.getInstance(Car.class);
            assertEquals("V8", car.getEngine().getName());
        }

        @Qualifier
        @Retention(RUNTIME)
        @Target({FIELD, PARAMETER, METHOD})
        public @interface Luxury {
        }

        record LuxuryLiteral() implements Luxury{
            @Override
            public Class<? extends Annotation> annotationType() {
                return Luxury.class;
            }
        }

        static class LuxuryCar implements Car {
            @Inject @Luxury
            private Engine engine;

            @Override
            public Engine getEngine() {
                return engine;
            }
        }

        @Test
        public void select_qualifier() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Engine.class).to(V8Engine.class);
                    bind(Engine.class).annotatedWith(new LuxuryLiteral()).to(V6Engine.class);
                    bind(Car.class).to(LuxuryCar.class);
                }
            });

            Car car = injector.getInstance(Car.class);
            assertEquals("V6", car.getEngine().getName());
        }
    }

    @Nested
    public class ContextInScope{
        @Test
        public void singleton() {
            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Engine.class).annotatedWith(Names.named("V8")).to(V8Engine.class).in(Scopes.SINGLETON);
                    bind(Engine.class).annotatedWith(Names.named("V6")).to(V6Engine.class);
                    bind(Car.class).to(DependencySelection.V8Car.class);
                }
            });

            Car car1 = injector.getInstance(Car.class);
            Car car2 = injector.getInstance(Car.class);

            assertNotSame(car1, car2);
            assertSame(car1.getEngine(), car2.getEngine());
        }
    }
}

