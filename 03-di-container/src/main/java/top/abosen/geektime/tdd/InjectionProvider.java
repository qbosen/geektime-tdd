package top.abosen.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private final Constructor<T> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);

        if (injectFields.stream().anyMatch(it -> Modifier.isFinal(it.getModifiers()))) {
            throw new IllegalComponentException();
        }
        if (injectMethods.stream().anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }

    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(
                stream(injectConstructor.getParameters()).map(Parameter::getType),
                concat(injectFields.stream().map(Field::getType),
                        injectMethods.stream().flatMap(it -> stream(it.getParameterTypes()))
                )).toList();
    }

    private static <T> Constructor<T> getInjectConstructor(Class<T> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalComponentException();
        }
    }

    private static <Type> List<Field> getInjectFields(Class<Type> component) {
        return traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
    }


    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> notOverrideByInjectMethod(methods, m))
                .filter(m -> notOverrideByNoInjectMethod(component, m))
                .toList());
        Collections.reverse(injectMethods);
        return injectMethods;
    }


    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> injects = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injects.addAll(finder.apply(injects, current));
            current = current.getSuperclass();
        }
        return injects;
    }

    private static Object toDependency(Context context, Field field) {
        return context.get(field.getType());
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameterTypes()).map(context::get).toArray();
    }

    private static <T> boolean notOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean notOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] annotatedElement) {
        return stream(annotatedElement).filter(it -> it.isAnnotationPresent(Inject.class));
    }
}
