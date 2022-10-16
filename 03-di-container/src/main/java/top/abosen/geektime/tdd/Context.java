package top.abosen.geektime.tdd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider(injectConstructor));
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

    public <Type> Type get(Class<Type> type) {
        return getOpt(type).orElseThrow(DependencyNotFountException::new);
    }

    public <Type> Optional<Type> getOpt(Class<Type> type) {
        return Optional.ofNullable(providers.get(type)).map(it -> ((Provider<Type>) it).get());
    }

    private final class ConstructorInjectionProvider<Type> implements Provider<Type> {
        private final Constructor<Type> injectConstructor;
        private boolean constructing = false;

        private ConstructorInjectionProvider(Constructor<Type> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CyclicDependenciesException();
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters()).map(parameter -> Context.this.get(parameter.getType())).toArray();
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }

    }
}
