package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

        @Test
        void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(AbstractComponent.class));
        }

        @Test
        void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(Component.class));
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
        public class ConstructorInjection {
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component instance = config.getContext().get(Component.class);
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);
            }

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);

                Component instance = config.getContext().get(Component.class);
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "indirect dependency");

                Component instance = config.getContext().get(Component.class);
                assertNotNull(instance);

                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);

                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());
            }


            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultiInjectConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithNoInjectNorDefaultConstructors.class);
                });
            }

            @Test
            public void should_throw_exception_if_dependency_not_exist() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> config.getContext());

                assertEquals(Dependency.class, exception.getDependency());
            }

            @Test
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFountException exception = assertThrows(DependencyNotFountException.class, () -> config.getContext());

                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
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

        @Nested
        public class FieldInjection {

            static class ComponentWithFieldInjection implements Component {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            void should_inject_dependency_via_field() {
                Dependency instance = new Dependency() {
                };
                config.bind(Component.class, ComponentWithFieldInjection.class);
                config.bind(Dependency.class, instance);

                Component component = config.getContext().get(Component.class);
                assertSame(instance, ((ComponentWithFieldInjection) component).dependency);
            }

            @Test
            void should_inject_dependency_via_superclass_inject_field() {
                Dependency instance = new Dependency() {
                };
                config.bind(Component.class, SubclassWithFieldInjection.class);
                config.bind(Dependency.class, instance);

                Component component = config.getContext().get(Component.class);
                assertSame(instance, ((SubclassWithFieldInjection) component).dependency);
            }

            @Test
            void should_create_component_with_injection_field() {
                Context context = Mockito.mock(Context.class);
                Dependency dependency = Mockito.mock(Dependency.class);
                Mockito.when(context.get(eq(Dependency.class))).thenReturn(dependency);
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);

                ComponentWithFieldInjection component = provider.get(context);
                assertSame(dependency, component.dependency);
            }


            // 依赖丢失,循环依赖的检测 均为外部系统完成,所以只用进行组件功能测试即可
            @Test
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        public class MethodInjection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            void should_call_inject_method() {
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
                InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class);
                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_dependency_via_inject_method() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);

                InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class);
                assertSame(dependency, component.dependency);
            }

            static class SuperClassWithInjectMethod {
                int supperCalled = 0;

                @Inject
                void install() {
                    supperCalled++;
                }

            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = supperCalled + 1;
                }
            }

            @Test
            void should_inject_dependencies_via_inject_method_from_superclass() {
                config.bind(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);
                SubclassWithInjectMethod component = config.getContext().get(SubclassWithInjectMethod.class);
                assertEquals(1, component.supperCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassOverrideSuperClassWithInjectMethod extends SuperClassWithInjectMethod {

                @Inject
                @Override
                void install() {
                    super.install();
                }
            }

            @Test
            void should_only_call_once_if_subclass_override_inject_method_with_inject() {
                config.bind(SubclassOverrideSuperClassWithInjectMethod.class, SubclassOverrideSuperClassWithInjectMethod.class);
                SubclassOverrideSuperClassWithInjectMethod component = config.getContext().get(SubclassOverrideSuperClassWithInjectMethod.class);
                assertEquals(1, component.supperCalled);
            }

            static class SubclassOverrideSuperClassWithNoInject extends SuperClassWithInjectMethod {
                @Override
                void install() {
                    super.install();
                }
            }

            @Test
            void should_not_call_inject_method_if_override_with_no_inject() {
                config.bind(SubclassOverrideSuperClassWithNoInject.class, SubclassOverrideSuperClassWithNoInject.class);
                SubclassOverrideSuperClassWithNoInject component = config.getContext().get(SubclassOverrideSuperClassWithNoInject.class);
                assertEquals(0, component.supperCalled);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray());
            }

            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(InjectMethodWithTypeParameter.class));
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