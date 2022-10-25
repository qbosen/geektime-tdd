package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContainerTest {


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

            assertSame(instance, config.getContext().get(Component.class));
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);

            assertSame(dependency, config.getContext().get(Component.class).dependency());
        }

        static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class))
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
            assertThrows(DependencyNotFountException.class, () -> config.getContext().get(Component.class));
        }

        @Test
        public void should_return_empty_if_component_not_defined_with_optional_get() {
            Optional<Component> component = config.getContext().getOpt(Component.class);
            assertTrue(component.isEmpty());
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
                    Arguments.of(Named.of("Inject Constructor", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", MissingDependencyMethod.class))
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
            Map<String, Class<?>> components = Map.of("Inject Constructor", CyclicComponentInjectConstructor.class,
                    "Inject Field", CyclicComponentInjectField.class,
                    "Inject Method", CyclicComponentInjectMethod.class);

            Map<String, Class<?>> dependencies = Map.of("Inject Constructor", CyclicDependencyInjectConstructor.class,
                    "Inject Field", CyclicDependencyInjectField.class,
                    "Inject Method", CyclicDependencyInjectMethod.class);

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

    }

    @Nested
    public class DependenciesSelectioin {


    }

    @Nested
    public class LifecycleManagement {


    }
}

interface Component {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency {
}

interface AnotherDependency {
}
