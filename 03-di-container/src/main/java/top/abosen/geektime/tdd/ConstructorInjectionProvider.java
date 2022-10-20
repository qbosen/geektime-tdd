package top.abosen.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private final Constructor<Type> injectConstructor;
    private final List<Field> injectFields;

    public ConstructorInjectionProvider(Class<Type> component) {
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Stream.concat(
                Arrays.stream(injectConstructor.getParameters()).map(Parameter::getType),
                injectFields.stream().map(Field::getType)
        ).toList();
    }

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(parameter -> context.get(parameter.getType())).toArray();
            Type instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()));
            }
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors())
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
            injectFields.addAll(Arrays.stream(current.getDeclaredFields()).filter(it -> it.isAnnotationPresent(Inject.class)).toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }
}
