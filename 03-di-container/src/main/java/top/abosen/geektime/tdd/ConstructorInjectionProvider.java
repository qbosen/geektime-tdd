package top.abosen.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class ConstructorInjectionProvider<Type> implements ContextConfig.ComponentProvider<Type> {
    private final Constructor<Type> injectConstructor;

    public ConstructorInjectionProvider(Class<Type> component) {
        this.injectConstructor = getInjectConstructor(component);
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.stream(injectConstructor.getParameters()).map(Parameter::getType).collect(Collectors.toList());
    }

    @Override
    public Type get(Context context) {
        try {
            Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                    .map(parameter -> context.get(parameter.getType())).toArray();
            return injectConstructor.newInstance(dependencies);
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
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalComponentException();
            }
        });
    }

}
