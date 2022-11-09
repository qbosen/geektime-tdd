package top.abosen.geektime.tdd.di;

import jakarta.inject.*;
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
import java.util.stream.IntStream;
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
            config.bindInstance(TestComponent.class, instance);

            assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)));
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bindInstance(Dependency.class, dependency);
            config.bindComponent(TestComponent.class, componentType);

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
        public void should_throw_exception_for_unbind_type() {
            assertThrows(ContextConfigError.class, () -> config.getContext().get(ComponentRef.of(TestComponent.class)));
        }

        @Test
        public void should_retrieve_empty_for_unbind_type_with_optional_get() {
            Optional<TestComponent> component = config.getContext().getOpt(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        void should_retrieve_component_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bindInstance(TestComponent.class, instance);

            Context context = config.getContext();

            assertSame(instance, context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bindInstance(TestComponent.class, instance);

            Context context = config.getContext();
            assertFalse(context.getOpt(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        class WithQualifier {
            private final TestComponent instance = new TestComponent() {
            };

            static class InjectConstructor {
                private Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_bind_instance_with_multi_qualifier() {

                config.bindInstance(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

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
                config.bindInstance(Dependency.class, dependency);
                config.bindComponent(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("ChosenOne"), new NamedLiteral("Skywalker"));

                Context context = config.getContext();
                InjectConstructor chosenOne = context.get(ComponentRef.of(InjectConstructor.class, new NamedLiteral("ChosenOne")));
                InjectConstructor skywalker = context.get(ComponentRef.of(InjectConstructor.class, new NamedLiteral("Skywalker")));
                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                assertThrows(IllegalComponentException.class, () -> config.bindInstance(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            void should_retrieve_component_bind_type_as_provider() {
                config.bindInstance(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                assertTrue(config.getContext().getOpt(new ComponentRef<Provider<TestComponent>>(new SkywalkerLiteral()) {
                }).isPresent());
            }

            @Test
            void should_retrieve_empty_if_no_matched_qualifiers() {
                config.bindInstance(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());
                assertTrue(config.getContext().getOpt(new ComponentRef<Provider<TestComponent>>() {
                }).isEmpty());
            }

            @Test
            void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bindComponent(InjectConstructor.class, InjectConstructor.class, new TestLiteral()));
            }
        }

        @Nested
        class WithScope {
            static class NotSingleton {
            }

            @Test
            void should_not_be_singleton_scope_by_default() {
                config.bindComponent(NotSingleton.class, NotSingleton.class);
                Context context = config.getContext();
                assertNotSame(context.get(ComponentRef.of(NotSingleton.class)), context.get(ComponentRef.of(NotSingleton.class)));
            }

            @Test
            void should_bind_component_as_singleton_scoped() {
                config.bindComponent(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(NotSingleton.class)), context.get(ComponentRef.of(NotSingleton.class)));
            }

            @Singleton
            static class SingletonAnnotated implements Dependency {

            }

            @Test
            void should_retrieve_scope_annotation_from_component() {
                config.bindComponent(Dependency.class, SingletonAnnotated.class);
                Context context = config.getContext();
                assertSame(context.get(ComponentRef.of(Dependency.class)), context.get(ComponentRef.of(Dependency.class)));
            }

            @Test
            void should_bind_component_as_customized_scope() {
                config.scope(Pooled.class, PooledProvider::new);
                config.bindComponent(NotSingleton.class, NotSingleton.class, new PooledLiteral());
                Context context = config.getContext();

                List<NotSingleton> pooled = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(NotSingleton.class))).distinct().toList();
                assertEquals(PooledProvider.MAX, pooled.size());
            }

            @Test
            void should_throw_exception_if_multi_scope_provided() {
                assertThrows(IllegalComponentException.class, () -> config.bindComponent(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new PooledLiteral()));
            }

            @Singleton
            @Pooled
            static class MultiScopeAnnotated {

            }

            @Test
            void should_throw_exception_if_multi_scope_annotated() {
                assertThrows(IllegalComponentException.class, () -> config.bindComponent(MultiScopeAnnotated.class, MultiScopeAnnotated.class));
            }

            @Test
            void should_throw_exception_if_scope_undefined() {
                assertThrows(IllegalComponentException.class, () -> config.bindComponent(NotSingleton.class, NotSingleton.class, new PooledLiteral()));
            }

            @Nested
            class WithQualifier {
                @Test
                void should_not_be_singleton_scope_by_default() {
                    config.bindComponent(NotSingleton.class, NotSingleton.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertNotSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())), context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())));
                }

                @Test
                void should_bind_component_as_singleton_scoped() {
                    config.bindComponent(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())), context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())));
                }

                @Test
                void should_retrieve_scope_annotation_from_component() {
                    config.bindComponent(Dependency.class, SingletonAnnotated.class, new SkywalkerLiteral());
                    Context context = config.getContext();
                    assertSame(context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())), context.get(ComponentRef.of(Dependency.class, new SkywalkerLiteral())));
                }
            }
        }
    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bindComponent(TestComponent.class, component);
            ContextConfigError error = assertThrows(ContextConfigError.class, () -> config.getContext());

            ContextConfigError.DependencyNotFount dependencyNotFount = assertInstanceOf(ContextConfigError.DependencyNotFount.class, error);
            assertEquals(Dependency.class, dependencyNotFount.dependency().type());
            assertEquals(TestComponent.class, dependencyNotFount.component().type());
        }

        static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Field", MissingDependencyProviderField.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class)),
                    Arguments.of(Named.of("Scope", MissingDependencyScope.class)),
                    Arguments.of(Named.of("Scope Provider", MissingDependencyScopeProvider.class))
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

        @Singleton
        static class MissingDependencyScope implements TestComponent {
            @Inject
            private Dependency dependency;
        }

        @Singleton
        static class MissingDependencyScopeProvider implements TestComponent {
            @Inject
            private Provider<Dependency> dependency;
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component, Class<? extends Dependency> dependency) {
            config.bindComponent(TestComponent.class, component);
            config.bindComponent(Dependency.class, dependency);

            ContextConfigError error = assertThrows(ContextConfigError.class, () -> config.getContext());

            ContextConfigError.CyclicDependenciesFound configError = assertInstanceOf(ContextConfigError.CyclicDependenciesFound.class, error);

            assertEquals(3, configError.getPath().length);
            Set<Class<?>> classes = Sets.newSet(configError.getPath());
            assertTrue(classes.containsAll(Arrays.asList(TestComponent.class, Dependency.class)));
        }

        static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            Map<String, Class<?>> components = Map.of("Inject Constructor", CyclicComponentInjectConstructor.class,
                    "Inject Field", CyclicComponentInjectField.class,
                    "Inject Method", CyclicComponentInjectMethod.class);

            Map<String, Class<?>> dependencies = Map.of("Inject Constructor", CyclicDependencyInjectConstructor.class,
                    "Inject Field", CyclicDependencyInjectField.class,
                    "Inject Method", CyclicDependencyInjectMethod.class);

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
            config.bindComponent(TestComponent.class, component);
            config.bindComponent(Dependency.class, dependency);
            config.bindComponent(AnotherDependency.class, anotherDependency);
            ContextConfigError error = assertThrows(ContextConfigError.class, () -> config.getContext());

            ContextConfigError.CyclicDependenciesFound configError = assertInstanceOf(ContextConfigError.CyclicDependenciesFound.class, error);
            assertEquals(4, configError.getPath().length);
            Set<Class<?>> classes = Sets.newSet(configError.getPath());
            assertTrue(classes.containsAll(Arrays.asList(TestComponent.class, Dependency.class, AnotherDependency.class)));
        }

        static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            Map<String, Class<?>> components = Map.of("Inject Constructor", CyclicComponentInjectConstructor.class,
                    "Inject Field", CyclicComponentInjectField.class,
                    "Inject Method", CyclicComponentInjectMethod.class);
            Map<String, Class<?>> dependencies = Map.of("Inject Constructor", CyclicIndirectDependencyInjectConstructor.class,
                    "Inject Field", CyclicIndirectDependencyInjectField.class,
                    "Inject Method", CyclicIndirectDependencyInjectMethod.class);
            Map<String, Class<?>> anotherDependencies = Map.of("Inject Constructor", CyclicIndirectAnotherDependencyInjectConstructor.class,
                    "Inject Field", CyclicIndirectAnotherDependencyInjectField.class,
                    "Inject Method", CyclicIndirectAnotherDependencyInjectMethod.class);


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
            config.bindComponent(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bindComponent(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.getOpt(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        class WithQualifier {
            @ParameterizedTest
            @MethodSource
            void should_throw_exception_if_dependency_with_qualifier_not_found(Class<? extends TestComponent> component) {
                config.bindInstance(Dependency.class, new Dependency() {
                });
                config.bindComponent(TestComponent.class, component, new NamedLiteral("Owner"));
                ContextConfigError error = assertThrows(ContextConfigError.class, () -> config.getContext());

                ContextConfigError.DependencyNotFount dependencyNotFount = assertInstanceOf(ContextConfigError.DependencyNotFount.class, error);
                assertEquals(new Component(TestComponent.class, new NamedLiteral("Owner")), dependencyNotFount.component());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), dependencyNotFount.dependency());
            }

            static Stream<Arguments> should_throw_exception_if_dependency_with_qualifier_not_found() {
                Map<String, Class<?>> map = Map.of(
                        "Inject Constructor with Qualifier", InjectConstructor.class,
                        "Inject Field with Qualifier", InjectField.class,
                        "Inject InjectMethod with Qualifier", InjectMethod.class,
                        "Provider in Inject Constructor with Qualifier", InjectConstructorProvider.class,
                        "Provider in Inject Field with Qualifier", InjectFieldProvider.class,
                        "Provider in Inject InjectMethod with Qualifier", InjectMethodProvider.class
                );
                return iterateMap(map).map(Arguments::of);
            }

            static class InjectConstructor implements TestComponent {
                @Inject
                public InjectConstructor(@Skywalker Dependency dependency) {
                }
            }

            static class InjectField implements TestComponent {
                @Inject
                @Skywalker
                Dependency dependency;
            }

            static class InjectMethod implements TestComponent {
                @Inject
                void install(@Skywalker Dependency dependency) {
                }
            }

            static class InjectConstructorProvider implements TestComponent {
                @Inject
                public InjectConstructorProvider(@Skywalker Provider<Dependency> dependency) {
                }
            }

            static class InjectFieldProvider implements TestComponent {
                @Inject
                @Skywalker
                Provider<Dependency> dependency;
            }

            static class InjectMethodProvider implements TestComponent {
                @Inject
                void install(@Skywalker Provider<Dependency> dependency) {
                }
            }


            @ParameterizedTest(name = "{1} -> @Skywalker({0}) -> @Named(\"ChosenOne\") not cyclic dependencies")
            @MethodSource
            void should_not_throw_exception_if_component_with_same_type_tagged_with_different_qualifier
                    (Class<? extends Dependency> skywalker, Class<? extends Dependency> notCyclic) {
                Dependency instance = new Dependency() {
                };
                config.bindInstance(Dependency.class, instance, new NamedLiteral("ChosenOne"));
                config.bindComponent(Dependency.class, skywalker, new SkywalkerLiteral());
                config.bindComponent(Dependency.class, notCyclic);

                assertDoesNotThrow(() -> config.getContext());
            }

            static Stream<Arguments> should_not_throw_exception_if_component_with_same_type_tagged_with_different_qualifier() {
                Map<String, Class<?>> skywalker = Map.of("Inject Constructor", SkywalkerInjectConstructor.class,
                        "Field Constructor", SkywalkerInjectField.class,
                        "Method Constructor", SkywalkerInjectMethod.class);

                Map<String, Class<?>> notCyclic = Map.of("Inject Constructor", NotCyclicInjectConstructor.class,
                        "Field Constructor", NotCyclicInjectField.class,
                        "Method Constructor", NotCyclicInjectMethod.class);
                return iterateMap(skywalker).flatMap(s -> iterateMap(notCyclic).map(n -> Arguments.of(s, n)));
            }

            static class SkywalkerInjectConstructor implements Dependency {
                @Inject
                public SkywalkerInjectConstructor(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class SkywalkerInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChosenOne")
                Dependency dependency;
            }

            static class SkywalkerInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectConstructor implements Dependency {
                @Inject
                public NotCyclicInjectConstructor(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

            static class NotCyclicInjectField implements Dependency {
                @Inject
                @jakarta.inject.Named("ChosenOne")
                Dependency dependency;
            }

            static class NotCyclicInjectMethod implements Dependency {
                @Inject
                void install(@jakarta.inject.Named("ChosenOne") Dependency dependency) {
                }
            }

        }
    }

    @Nested
    class DSL {
        interface Api {
        }

        static class Implementation implements Api {
        }

        @Test
        void should_bind_instance_as_its_declaration_type() {
            Implementation instance = new Implementation();
            config.from(new Config() {
                Implementation implementation = instance;
            });

            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(Implementation.class)));
        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
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
@Retention(RUNTIME)
@interface Skywalker {
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

@Scope
@Documented
@Retention(RUNTIME)
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

class PooledProvider<T> implements ComponentProvider<T> {
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