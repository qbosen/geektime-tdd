package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

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
    public class ComponentConstruction {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);

            assertSame(instance, config.getContext().get(Component.class));
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

        @Nested
        public class DependencyCheck {
            @Test
            public void should_throw_exception_if_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> config.getContext());

                assertEquals(Dependency.class, exception.getDependency());
                assertEquals(Component.class, exception.getComponent());
            }


            @Test
            public void should_throw_exception_if_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependedOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                assertEquals(3, exception.getComponents().length);
                Set<Class<?>> classes = Sets.newSet(exception.getComponents());
                assertTrue(classes.containsAll(Arrays.asList(Component.class, Dependency.class)));
            }


            @Test
            public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyDependsOnAnotherDependency.class);
                config.bind(AnotherDependency.class, AnotherDependencyDependOnComponent.class);

                CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

                assertEquals(4, exception.getComponents().length);
                Set<Class<?>> classes = Sets.newSet(exception.getComponents());
                assertTrue(classes.containsAll(Arrays.asList(Component.class, Dependency.class, AnotherDependency.class)));
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
}

interface Dependency {
}

interface AnotherDependency {
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

 class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }
}