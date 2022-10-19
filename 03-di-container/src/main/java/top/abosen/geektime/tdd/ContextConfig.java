package top.abosen.geektime.tdd;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qiubaisen
 * @date 2022/10/14
 */
public class ContextConfig {

    interface ComponentProvider<T> {
        T get(Context context);

        List<Class<?>> getDependencies();
    }

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return Arrays.asList();
            }
        });
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<Implementation> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider(injectConstructor));
    }

    public Context getContext() {
        providers.keySet().forEach(component -> checkDependencies(component, new ArrayDeque<>()));
        return new Context() {
            @Override
            public <Type> Type get(Class<Type> type) {
                return getOpt(type).orElseThrow(() -> new DependencyNotFountException(type, type));
            }

            @Override
            public <Type> Optional<Type> getOpt(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(it -> (Type) it.get(this));
            }
        };
    }

    private void checkDependencies(Class<?> component, Deque<Class<?>> visiting) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency)) {
                throw new DependencyNotFountException(dependency, component);
            }
            if (visiting.contains(dependency)) {
                List<Class<?>> cyclicPath = new ArrayList<>(visiting);
                cyclicPath.add(dependency);
                throw new CyclicDependenciesFoundException(cyclicPath);
            }
            visiting.addLast(dependency);
            checkDependencies(dependency, visiting);
            visiting.removeLast();
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

    private static final class ConstructorInjectionProvider<Type> implements ComponentProvider<Type> {
        private final Constructor<Type> injectConstructor;

        public ConstructorInjectionProvider(Constructor<Type> injectConstructor) {
            this.injectConstructor = injectConstructor;
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

    }
}
