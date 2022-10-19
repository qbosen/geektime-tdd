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
public class ContextConfig {
    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider(type, injectConstructor));
    }

    public Context getContext(){
        return new Context() {
            @Override
            public <Type> Type get(Class<Type> type) {
                return getOpt(type).orElseThrow(() -> new DependencyNotFountException(type, type));
            }

            @Override
            public <Type> Optional<Type> getOpt(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> ((Provider<Type>) it).get());
            }
        };
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

    private final class ConstructorInjectionProvider<Type> implements Provider<Type> {
        private Class<?> componentType;
        private final Constructor<Type> injectConstructor;
        private boolean constructing = false;

        public ConstructorInjectionProvider(Class<?> component, Constructor<Type> injectConstructor) {
            this.componentType = component;
            this.injectConstructor = injectConstructor;
        }

        @Override
        public Type get() {
            if (constructing) {
                throw new CyclicDependenciesFoundException(componentType);
            }
            try {
                constructing = true;
                Object[] dependencies = Arrays.stream(injectConstructor.getParameters())
                        .map(parameter -> getContext().getOpt(parameter.getType())
                                .orElseThrow(() -> new DependencyNotFountException(parameter.getType(), componentType))
                        ).toArray();
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (CyclicDependenciesFoundException e) {
                throw new CyclicDependenciesFoundException(componentType, e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } finally {
                constructing = false;
            }
        }

    }
}
