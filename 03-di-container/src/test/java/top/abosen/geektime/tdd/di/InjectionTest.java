package top.abosen.geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/10/21
 */
@Nested
public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);
    private Context context = mock(Context.class);
    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private ParameterizedType dependencyProviderType;

    @BeforeEach
    public void setup() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(dependency);
        Mockito.<Provider<Dependency>>when(context.get(eq(ComponentRef.of(dependencyProviderType)))).thenReturn(dependencyProvider);
    }


    @Nested
    public class ConstructorInjection {
        @Nested
        class Injection {

            static class DefaultConstructor implements TestComponent {
                public DefaultConstructor() {
                }
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                InjectionProvider<DefaultConstructor> provider = new InjectionProvider<>(DefaultConstructor.class);
                DefaultConstructor instance = provider.get(context);
                assertNotNull(instance);
            }

            static class InjectConstructor {
                private Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.dependency);
            }

            @Test
            void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            @Test
            void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectConstructors {
            abstract class AbstractComponent implements TestComponent {
                @Inject
                public AbstractComponent() {
                }
            }

            class ComponentWithMultiInjectConstructors implements TestComponent {
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

            class ComponentWithNoInjectNorDefaultConstructors implements TestComponent {
                public ComponentWithNoInjectNorDefaultConstructors(String name) {
                }
            }


            @Test
            void should_throw_exception_if_component_is_abstract() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(AbstractComponent.class));
            }

            @Test
            void should_throw_exception_if_component_is_interface() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(TestComponent.class));
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(ComponentWithMultiInjectConstructors.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(ComponentWithNoInjectNorDefaultConstructors.class));
            }

        }

        @Nested
        class WithQualifier {

            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(dependency);
            }

            static class InjectConstructor {

                private final Dependency dependency;

                @Inject
                public InjectConstructor(@Named("ChosenOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_dependency_with_qualifier_via_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                InjectConstructor component = provider.get(context);
                assertSame(dependency, component.dependency);
            }


            @Test
            void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectConstructor {
                @Inject
                public MultiQualifierInjectConstructor(@Named("ChoseOne") @Skywalker Dependency dependency) {
                }
            }

            @Test
            void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(MultiQualifierInjectConstructor.class));
            }
        }

    }

    @Nested
    public class FieldInjection {

        @Nested
        class Injection {
            static class ComponentWithFieldInjection implements TestComponent {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            void should_inject_dependency_via_field() {

                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                ComponentWithFieldInjection component = provider.get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_inject_dependency_via_superclass_inject_field() {

                InjectionProvider<SubclassWithFieldInjection> provider = new InjectionProvider<>(SubclassWithFieldInjection.class);
                SubclassWithFieldInjection component = provider.get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_from_field_dependencies() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


            @Test
            void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;
            }

            @Test
            void should_inject_provider_via_inject_field() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectFields {
            static class FinalInjectField {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            void should_throw_exception_if_inject_field_is_final() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(FinalInjectField.class));
            }
        }

        @Nested
        class WithQualifier {
            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(dependency);
            }


            static class InjectField {
                @Inject
                @Named("ChosenOne")
                Dependency dependency;

            }

            @Test
            void should_inject_dependency_with_qualifier_via_field() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                InjectField component = provider.get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectField> provider = new InjectionProvider<>(InjectField.class);
                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectField {
                @Inject
                @Named("ChoseOne")
                @Skywalker
                Dependency dependency;
            }

            @Test
            void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(MultiQualifierInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            void should_call_inject_method_even_if_no_dependency_declared() {
                InjectionProvider<InjectMethodWithNoDependency> provider = new InjectionProvider<>(InjectMethodWithNoDependency.class);
                InjectMethodWithNoDependency component = provider.get(context);
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
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                InjectMethodWithDependency component = provider.get(context);
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
                InjectionProvider<SubclassWithInjectMethod> provider = new InjectionProvider<>(SubclassWithInjectMethod.class);
                SubclassWithInjectMethod component = provider.get(context);
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
                InjectionProvider<SubclassOverrideSuperClassWithInjectMethod> provider = new InjectionProvider<>(SubclassOverrideSuperClassWithInjectMethod.class);
                SubclassOverrideSuperClassWithInjectMethod component = provider.get(context);
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
                InjectionProvider<SubclassOverrideSuperClassWithNoInject> provider = new InjectionProvider<>(SubclassOverrideSuperClassWithNoInject.class);
                SubclassOverrideSuperClassWithNoInject component = provider.get(context);

                assertEquals(0, component.supperCalled);
            }

            static class SubclassExtendsNoInjectOverrideSubclass extends SubclassOverrideSuperClassWithNoInject {
            }

            @Test
            void should_not_call_inject_method_if_extends_no_inject_override_method() {
                InjectionProvider<SubclassExtendsNoInjectOverrideSubclass> provider = new InjectionProvider<>(SubclassExtendsNoInjectOverrideSubclass.class);
                SubclassExtendsNoInjectOverrideSubclass component = provider.get(context);

                assertEquals(0, component.supperCalled);
            }

            @Test
            void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            @Test
            void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(dependencyProviderType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_provider_via_inject_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }
        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }

        @Nested
        class WithQualifier {

            @BeforeEach
            void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))))).thenReturn(dependency);
            }

            static class InjectMethod {

                private Dependency dependency;

                @Inject
                void install(@Named("ChosenOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            void should_inject_dependency_with_qualifier_via_method() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                InjectMethod component = provider.get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);
                assertArrayEquals(new ComponentRef<?>[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            static class MultiQualifierInjectMethod {
                @Inject
                void install(@Named("ChoseOne") @Skywalker Dependency dependency) {
                }
            }

            @Test
            void should_throw_exception_if_multi_qualifiers_given() {
                assertThrows(ComponentError.class, () -> new InjectionProvider<>(MultiQualifierInjectMethod.class));
            }
        }
    }


}
