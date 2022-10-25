package top.abosen.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private final Constructor<Type> injectConstructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<Type> component) {
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
    public List<Class<?>> getDependencies() {
        return concat(
                stream(injectConstructor.getParameters()).map(Parameter::getType),
                concat(
                        injectFields.stream().map(Field::getType),
                        injectMethods.stream().flatMap(it -> stream(it.getParameterTypes()))
                )).toList();
    }

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = stream(injectConstructor.getParameters())
                    .map(parameter -> context.get(parameter.getType())).toArray();
            Type instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.setAccessible(true);
                field.set(instance, context.get(field.getType()));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, stream(method.getParameterTypes()).map(context::get).toArray());
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(it -> it.isAnnotationPresent(Inject.class)).toList();

        if (injectConstructors.size() > 1) {
            throw new IllegalComponentException();
        }

        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

    private static <Type> List<Field> getInjectFields(Class<Type> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(stream(current.getDeclaredFields()).filter(it -> it.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> List<Method> getInjectMethods(Class<Type> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(stream(current.getDeclaredMethods())
                    .filter(it -> it.isAnnotationPresent(Inject.class))
                    .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName())
                            && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
                    .filter(m -> stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                            .noneMatch(o -> o.getName().equals(m.getName())
                                    && Arrays.equals(o.getParameterTypes(), m.getParameterTypes())))
                    .toList()
            );
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }
}
