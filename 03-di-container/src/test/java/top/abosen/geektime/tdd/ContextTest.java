package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/10/27
 */
@Nested
class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }


    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)));
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            assertSame(dependency, ((TestComponent) config.getContext().get(ComponentRef.of(TestComponent.class))).dependency());
        }

        static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class))
            );
        }

        static class ConstructorInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            @Inject
            private Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }

        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return dependency;
            }
        }


        @Test
        public void should_throw_exception_if_component_not_defined() {
            assertThrows(DependencyNotFountException.class, () -> config.getContext().get(ComponentRef.of(TestComponent.class)));
        }

        @Test
        public void should_return_empty_if_component_not_defined_with_optional_get() {
            Optional<TestComponent> component = config.getContext().getOpt(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        void should_retrieve_component_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            assertSame(instance, context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();
            assertFalse(context.getOpt(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        class WithQualifier {

            static class InjectConstructor {
                private Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_bind_instance_with_multi_qualifier() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                Context context = config.getContext();
                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne")));
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral()));
                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            void should_bind_component_with_multi_qualifier() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("ChosenOne"), new NamedLiteral("Skywalker"));

                Context context = config.getContext();
                InjectConstructor chosenOne = context.get(ComponentRef.of(InjectConstructor.class, new NamedLiteral("ChosenOne")));
                InjectConstructor skywalker = context.get(ComponentRef.of(InjectConstructor.class, new NamedLiteral("Skywalker")));
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(InjectConstructor.class, InjectConstructor.class, new TestLiteral()));
            }
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);
            DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(TestComponent.class, exception.getComponent());
        }

        static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", DependencyCheck.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", DependencyCheck.MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", DependencyCheck.MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", DependencyCheck.MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", DependencyCheck.MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", DependencyCheck.MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements TestComponent {
            @Inject
            private Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component, Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            assertEquals(3, exception.getComponents().length);
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertTrue(classes.containsAll(Arrays.asList(TestComponent.class, Dependency.class)));
        }

        static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            Map<String, Class<?>> components = Map.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class,
                    "Inject Field", DependencyCheck.CyclicComponentInjectField.class,
                    "Inject Method", DependencyCheck.CyclicComponentInjectMethod.class);

            Map<String, Class<?>> dependencies = Map.of("Inject Constructor", DependencyCheck.CyclicDependencyInjectConstructor.class,
                    "Inject Field", DependencyCheck.CyclicDependencyInjectField.class,
                    "Inject Method", DependencyCheck.CyclicDependencyInjectMethod.class);

            return iterateMap(components).flatMap(component -> iterateMap(dependencies).map(dependency -> Arguments.of(component, dependency)));
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            private TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(TestComponent component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(
                Class<? extends TestComponent> component, Class<? extends Dependency> dependency, Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            assertEquals(4, exception.getComponents().length);
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertTrue(classes.containsAll(Arrays.asList(TestComponent.class, Dependency.class, AnotherDependency.class)));
        }

        static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            Map<String, Class<?>> components = Map.of("Inject Constructor", DependencyCheck.CyclicComponentInjectConstructor.class,
                    "Inject Field", DependencyCheck.CyclicComponentInjectField.class,
                    "Inject Method", DependencyCheck.CyclicComponentInjectMethod.class);
            Map<String, Class<?>> dependencies = Map.of("Inject Constructor", DependencyCheck.CyclicIndirectDependencyInjectConstructor.class,
                    "Inject Field", DependencyCheck.CyclicIndirectDependencyInjectField.class,
                    "Inject Method", DependencyCheck.CyclicIndirectDependencyInjectMethod.class);
            Map<String, Class<?>> anotherDependencies = Map.of("Inject Constructor", DependencyCheck.CyclicIndirectAnotherDependencyInjectConstructor.class,
                    "Inject Field", DependencyCheck.CyclicIndirectAnotherDependencyInjectField.class,
                    "Inject Method", DependencyCheck.CyclicIndirectAnotherDependencyInjectMethod.class);


            return iterateMap(components).flatMap(component ->
                    iterateMap(dependencies).flatMap(dependency ->
                            iterateMap(anotherDependencies).map(anotherDependency -> Arguments.of(component, dependency, anotherDependency))));
        }

        static Stream<Named<?>> iterateMap(Map<String, Class<?>> map) {
            return map.entrySet().stream().map(e -> Named.of(e.getKey(), e.getValue()));
        }


        static class CyclicIndirectDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicIndirectDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class CyclicIndirectDependencyInjectField implements Dependency {
            @Inject
            private AnotherDependency anotherDependency;
        }

        static class CyclicIndirectDependencyInjectMethod implements Dependency {
            @Inject
            void install(AnotherDependency anotherDependency) {
            }
        }

        static class CyclicIndirectAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public CyclicIndirectAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicIndirectAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            private TestComponent component;
        }

        static class CyclicIndirectAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> dependency) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.getOpt(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        class WithQualifier {
            //TODO dependency missing if qualifier not match
            //TODO check cyclic dependencies with qualifier
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }
}

@Qualifier
@Documented
@Retention(RUNTIME)
@interface Skywalker {
}

record SkywalkerLiteral() implements Skywalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }
}

record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}