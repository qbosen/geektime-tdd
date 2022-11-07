package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private final List<Injectable<Method>> injectMethods;
    private final List<Injectable<Field>> injectFields;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(component);
        this.injectMethods = getInjectMethods(component);
        this.injectFields = getInjectFields(component);

        if (injectFields.stream().map(Injectable::element).anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }


    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Injectable<Field> field : injectFields) {
                field.element().setAccessible(true);
                field.element().set(instance, field.toDependencies(context)[0]);
            }
            for (Injectable<Method> method : injectMethods) {
                method.element().invoke(instance, method.toDependencies(context));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static record Injectable<T extends AccessibleObject>(T element, ComponentRef<?>[] required) {
        private static <T extends Executable> Injectable<T> of(T element) {
            return new Injectable<>(element, stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new));
        }

        private static Injectable<Field> of(Field element) {
            return new Injectable<>(element, new ComponentRef[]{toComponentRef(element)});
        }


        Object[] toDependencies(Context context) {
            return stream(required()).map(context::get).toArray();
        }

        private static ComponentRef<Object> toComponentRef(Field field) {
            return ComponentRef.of(field.getGenericType(), getQualifier(field));
        }

        private static ComponentRef<Object> toComponentRef(Parameter parameter) {
            return ComponentRef.of(parameter.getParameterizedType(), getQualifier(parameter));
        }

        private static Annotation getQualifier(AnnotatedElement parameter) {
            List<Annotation> qualifiers = stream(parameter.getAnnotations()).filter(it -> it.annotationType().isAnnotationPresent(Qualifier.class)).toList();
            if (qualifiers.size() > 1) {
                throw new IllegalComponentException();
            }
            return qualifiers.stream().findFirst().orElse(null);
        }
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        List<Constructor<?>> injectConstructors = injectable(component.getConstructors()).toList();

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return Injectable.of((Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(component)));
    }

    private static List<Injectable<Field>> getInjectFields(Class<?> component) {
        List<Field> injectFields = traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
        return injectFields.stream().map(Injectable::of).toList();
    }

    private static List<Injectable<Method>> getInjectMethods(Class<?> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isLeafMethod(component, m))
//                .filter(m -> notOverrideByInjectMethod(methods, m))
//                .filter(m -> notOverrideByNoInjectMethod(component, m))
                .toList());
        // 父类的inject方法先执行, 但查找是从子类开始的,所以翻转一次
        Collections.reverse(injectMethods);
        return injectMethods.stream().map(Injectable::of).toList();
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return concat(concat(Stream.of(injectConstructor), injectFields.stream()), injectMethods.stream())
                .flatMap(it -> stream(it.required())).toList();
    }

    private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }


    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> visited = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            visited.addAll(finder.apply(visited, current));
            current = current.getSuperclass();
        }
        return visited;
    }

    private static boolean isLeafMethod(Class<?> component, Method method) {
        Class<?> current = component;
        while (current != Object.class) {
            Optional<Method> lowest = stream(current.getDeclaredMethods()).filter(m -> isOverride(m, method)).findFirst();
            if (lowest.isPresent()) {
                return lowest.get().equals(method);
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static boolean notOverrideByNoInjectMethod(Class<?> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean notOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    /**
     * 先不考虑private等问题
     */
    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] annotatedElement) {
        return stream(annotatedElement).filter(it -> it.isAnnotationPresent(Inject.class));
    }
}
