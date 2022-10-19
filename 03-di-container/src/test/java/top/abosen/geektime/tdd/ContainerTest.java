package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContainerTest {


    ContextConfig contextConfig;

    @BeforeEach
    public void setup() {
        contextConfig = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {
        //todo: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            contextConfig.bind(Component.class, instance);

            assertSame(instance, contextConfig.getContext().get(Component.class));
        }

        //todo: abstract class
        //todo: interface

        //todo: components does not exist
        @Test
        public void should_throw_exception_if_component_not_defined() {
            assertThrows(DependencyNotFountException.class, () -> contextConfig.getContext().get(Component.class));
        }

        @Test
        public void should_return_empty_if_component_not_defined_with_optional_get() {
            Optional<Component> component = contextConfig.getContext().getOpt(Component.class);
            assertTrue(component.isEmpty());
        }

        @Nested
        public class ConstructorInjection {
            //todo: no args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                contextConfig.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = contextConfig.getContext().get(Component.class);
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            //todo: with dependencies
            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, dependency);

                Component instance = contextConfig.getContext().get(Component.class);
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            //todo: A -> B -> C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);
                contextConfig.bind(String.class, "indirect dependency");

                Component instance = contextConfig.getContext().get(Component.class);
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }


            //todo: multi inject constructor
            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    contextConfig.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            //todo: no default constructor
            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    contextConfig.bind(Component.class, ComponentWithNoInjectNorDefaultConstructors.class);
                });
            }

            //todo: dependencies not exist
            @Test
            public void should_throw_exception_if_dependency_not_exist() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> contextConfig.getContext());

                assertEquals(Dependency.class, exception.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> contextConfig.getContext());

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
            }

            // todo A -> B -> A
            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> contextConfig.getContext());

                assertEquals(3, exception.getComponents().length);
                assertArrayEquals(new Class[]{Component.class, Dependency.class, Component.class}, exception.getComponents());

            }



            // todo A -> B -> C -> A
            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                contextConfig.bind(Component.class, ComponentWithInjectConstructor.class);
                contextConfig.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
                contextConfig.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> contextConfig.getContext());

                assertEquals(4, exception.getComponents().length);
                assertArrayEquals(new Class[]{Component.class, Dependency.class, AnotherDependency.class, Component.class}, exception.getComponents());
            }
        }

        @Nested
        public class FieldInjection {
        }

        @Nested
        public class MethodInjection {
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
}

interface Dependency {
}

interface AnotherDependency {
}

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }
}

class ComponentWithMultiInjectConstructors implements Component {
    private String name;
    private Double value;

    @Inject
    public ComponentWithMultiInjectConstructors(String name) {
        this.name = name;
    }

    @Inject
    public ComponentWithMultiInjectConstructors(String name, Double value) {
        this.name = name;
        this.value = value;
    }
}

class ComponentWithNoInjectNorDefaultConstructors implements Component {
    public ComponentWithNoInjectNorDefaultConstructors(String name) {
    }
}

class DependencyWithInjectConstructor implements Dependency {
    private String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
        return dependency;
    }
}

class DependencyDependedOnComponent implements Dependency {
    private Component component;

    @Inject
    public DependencyDependedOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class AnotherDependencyDependOnComponent implements AnotherDependency {
    private Component component;

    @Inject
    public AnotherDependencyDependOnComponent(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}

class DependencyDependsOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependsOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }

    public AnotherDependency getAnotherDependency() {
        return anotherDependency;
    }
}