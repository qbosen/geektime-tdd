package top.abosen.geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static top.abosen.geektime.tdd.di.ComponentError.*;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
final class InjectionProvider<T> implements ComponentProvider<T> {
    private final Injectable<Constructor<T>> injectConstructor;
    private final Map<Class<?>, List<Injectable<Method>>> injectMethods;
    private final Map<Class<?>, List<Injectable<Field>>> injectFields;
    private final Collection<Class<?>> superClasses;
    private final List<ComponentRef<?>> dependencies;

    public InjectionProvider(Class<T> component) {
        this.injectConstructor = getInjectConstructor(component);

        List<Injectable<Method>> methods = getInjectMethods(component);
        List<Injectable<Field>> fields = getInjectFields(component);

        this.injectMethods = groupByClass(methods);
        this.injectFields = groupByClass(fields);
        this.superClasses = allSuperClass(component);
        this.dependencies = concat(concat(Stream.of(injectConstructor), fields.stream()), methods.stream()).flatMap(it -> stream(it.required())).toList();
    }

    private static Collection<Class<?>> allSuperClass(Class<?> component) {
        LinkedList<Class<?>> top2bottom = new LinkedList<>();
        for (Class<?> current = component; current != Object.class; current = current.getSuperclass()) {
            top2bottom.addFirst(current);
        }
        return top2bottom;
    }

    private <E extends AccessibleObject & Member> Map<Class<?>, List<Injectable<E>>> groupByClass(List<Injectable<E>> elements) {
        return elements.stream().collect(Collectors.groupingBy(it -> it.element().getDeclaringClass(), Collectors.toList()));
    }


    @Override
    public T get(Context context) {
        try {
            T instance = injectConstructor.element().newInstance(injectConstructor.toDependencies(context));
            for (Class<?> c : superClasses) {
                for (Injectable<Field> field : injectFields.getOrDefault(c, List.of())) {
                    field.element().set(instance, field.toDependencies(context)[0]);
                }
                for (Injectable<Method> method : injectMethods.getOrDefault(c, List.of())) {
                    method.element().invoke(instance, method.toDependencies(context));
                }
            }

            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    static record Injectable<T extends AccessibleObject>(T element, ComponentRef<?>[] required) {
        Injectable {
            element.setAccessible(true);
        }

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
                throw ambiguousQualifiers(parameter, qualifiers);
            }
            return qualifiers.stream().findFirst().orElse(null);
        }
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw abstractComponent(component);
        }
        List<Constructor<?>> injectConstructors = injectable(component.getDeclaredConstructors()).toList();
        if (injectConstructors.size() > 1) {
            throw ambiguousInjectableConstructors(component);
        }

        return Injectable.of((Constructor<T>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(component)));
    }

    private static List<Injectable<Field>> getInjectFields(Class<?> component) {
        List<Injectable<Field>> injectables = InjectionProvider.<Field>traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList()).stream().map(Injectable::of).toList();
        return check(component, injectables, InjectionProvider::notFinal, ComponentError::finalInjectFields);
    }

    private static List<Injectable<Method>> getInjectMethods(Class<?> component) {

        List<Injectable<Method>> injectables = InjectionProvider.<Method>traverse(component, (methods, current) -> injectable(current.getDeclaredMethods())
                .filter(m -> isLeafMethod(component, m))
//                .filter(m -> notOverrideByInjectMethod(methods, m))
//                .filter(m -> notOverrideByNoInjectMethod(component, m))
                .toList()).stream().map(Injectable::of).toList();

        return check(component, injectables, InjectionProvider::noTypeParameter, ComponentError::injectMethodsWithTypeParameter);
    }

    @Override
    public List<ComponentRef<?>> getDependencies() {
        return this.dependencies;
    }

    private static <T> Constructor<T> defaultConstructor(Class<T> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw noDefaultConstructor(implementation);
        }
    }

    private static <E extends AccessibleObject> List<Injectable<E>> check(
            Class<?> component, List<Injectable<E>> target, Predicate<E> predicate, BiFunction<Class<?>, List<E>, ComponentError> error) {
        List<E> found = target.stream().map(Injectable::element).filter(predicate).toList();
        if (found.size() > 0) throw error.apply(component, found.stream().toList());
        return target;
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
        return true;
    }

    private static boolean notOverrideByNoInjectMethod(Class<?> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class))
                .noneMatch(o -> isOverride(m, o));
    }

    private static boolean notOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverride(Method m, Method o) {
        boolean visible;
        if (m.getDeclaringClass().getPackageName().equals(o.getDeclaringClass().getPackageName())) {
            visible = !Modifier.isPrivate(m.getModifiers()) && !Modifier.isPrivate(o.getModifiers());
        } else {
            visible = (Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) &&
                    (Modifier.isPublic(o.getModifiers()) || Modifier.isProtected(o.getModifiers()));
        }
        return visible && o.getName().equals(m.getName()) && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] annotatedElement) {
        return stream(annotatedElement).filter(it -> it.isAnnotationPresent(Inject.class));
    }

    private static boolean notFinal(Field field) {
        return Modifier.isFinal(field.getModifiers());
    }

    private static boolean noTypeParameter(Method method) {
        return method.getTypeParameters().length != 0;
    }
}

class ComponentError extends Error {
    public static ComponentError abstractComponent(Class<?> component) {
        return new ComponentError(MessageFormat.format("Can not be abstract: {0}", component));
    }

    public static ComponentError finalInjectFields(Class<?> component, Collection<Field> fields) {
        return new ComponentError(MessageFormat.format("Injectable field can not be final: {0} in {1}",
                String.join(" , ", fields.stream().map(Field::getName).toList()), component));
    }

    public static ComponentError injectMethodsWithTypeParameter(Class<?> component, Collection<Method> fields) {
        return new ComponentError(MessageFormat.format("Injectable method can not have type parameter: {0} in {1}",
                String.join(" , ", fields.stream().map(Method::getName).toList()), component));
    }

    public static ComponentError ambiguousInjectableConstructors(Class<?> component) {
        return new ComponentError(MessageFormat.format("Ambiguous injectable constructors: {0}", component));
    }

    public static ComponentError noDefaultConstructor(Class<?> component) {
        return new ComponentError(MessageFormat.format("No default constructors: {0}", component));
    }

    public static ComponentError ambiguousQualifiers(AnnotatedElement element, List<Annotation> qualifiers) {
        Class<?> component;
        if (element instanceof Parameter p) component = p.getDeclaringExecutable().getDeclaringClass();
        else component = ((Field) element).getDeclaringClass();
        return new ComponentError(MessageFormat.format("Ambiguous qualifiers: {0} on {1} of {2}",
                String.join(" , ", qualifiers.stream().map(Object::toString).toList()), element, component));
    }

    ComponentError(String message) {
        super(message);
    }
}