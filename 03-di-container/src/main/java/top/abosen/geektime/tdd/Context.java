package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class Context {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?>[] injectConstructors = Arrays.stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class)).toArray(Constructor[]::new);
        if (injectConstructors.length > 1) {
            throw new IllegalComponentException();
        }
        if (injectConstructors.length == 0 && Arrays.stream(implementation.getConstructors()).noneMatch(it -> it.getParameters().length == 0)) {
            throw new IllegalComponentException();
        }

        providers.put(type, (Provider<Implementation>) () -> {
            try {
                Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters()).map(parameter -> get(parameter.getType())).toArray();
                return (Implementation) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        Stream<Constructor<?>> injectConstructors = Arrays.stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class));

        return (Constructor<Type>) injectConstructors.findFirst().orElseGet(() -> {
            try {
                return implementation.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }
}
