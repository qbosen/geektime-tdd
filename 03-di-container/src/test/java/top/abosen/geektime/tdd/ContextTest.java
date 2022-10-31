package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Stream;

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
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Context.Ref.of(Component.class)));
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            assertSame(dependency, ((Component) config.getContext().get(Context.Ref.of(Component.class))).dependency());
        }

        static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class))
            );
        }

        static class ConstructorInjection implements Component {
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

        static class FieldInjection implements Component {
            @Inject
            private Dependency dependency;

            @Override
            public Dependency dependency() {
                return dependency;
            }

        }

        static class MethodInjection implements Component {
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
            assertThrows(DependencyNotFountException.class, () -> config.getContext().get(Context.Ref.of(Component.class)));
        }

        @Test
        public void should_return_empty_if_component_not_defined_with_optional_get() {
            Optional<Component> component = config.getContext().getOpt(Context.Ref.of(Component.class));
            assertTrue(component.isEmpty());
        }

        @Test
        void should_retrieve_component_bind_type_as_provider() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();

            assertSame(instance, context.get(new Context.Ref<Provider<Component>>() {
            }).get());
        }

        @Test
        void should_not_retrieve_bind_type_as_unsupported_container() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            Context context = config.getContext();
            assertFalse(context.getOpt(new Context.Ref<List<Component>>() {}).isPresent());
        }

    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());
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

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            private Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements Component {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderField implements Component {
            @Inject
            private Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements Component {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component, Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            assertEquals(3, exception.getComponents().length);
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertTrue(classes.containsAll(Arrays.asList(Component.class, Dependency.class)));
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

        static class CyclicComponentInjectConstructor implements Component {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements Component {
            @Inject
            private Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements Component {
            @Inject
            void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            private Component component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            void install(Component component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0}, {1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(
                Class<? extends Component> component, Class<? extends Dependency> dependency, Class<? extends AnotherDependency> anotherDependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            assertEquals(4, exception.getComponents().length);
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertTrue(classes.containsAll(Arrays.asList(Component.class, Dependency.class, AnotherDependency.class)));
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
            public CyclicIndirectAnotherDependencyInjectConstructor(Component component) {
            }
        }

        static class CyclicIndirectAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            private Component component;
        }

        static class CyclicIndirectAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            void install(Component component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<Component> dependency) {
            }
        }

        @Test
        void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(Component.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.getOpt(Context.Ref.of(Component.class)).isPresent());
        }
    }
}
