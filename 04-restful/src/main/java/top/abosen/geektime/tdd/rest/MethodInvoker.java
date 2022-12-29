package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static top.abosen.geektime.tdd.rest.MethodInvoker.ValueConverter.singleValued;

/**
 * @author qiubaisen
 * @date 2022/12/29
 */
class MethodInvoker {

    private static ValueProvider pathParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(PathParam.class))
            .map(annotation -> uriInfo.getPathParameters().get(annotation.value()));
    private static ValueProvider queryParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(QueryParam.class))
            .map(annotation -> uriInfo.getQueryParameters().get(annotation.value()));
    private static final List<ValueProvider> providers = List.of(pathParam, queryParam);

    static Object invoke(Method method, ResourceContext resourceContext, UriInfoBuilder builder) {
        try {
            UriInfo uriInfo = builder.createUriInfo();
            Object[] parameters = Arrays.stream(method.getParameters()).map(parameter ->
                            injectParameter(parameter, uriInfo)
                                    .or(() -> injectContext(parameter, resourceContext, uriInfo))
                                    .orElse(null)
                    )
                    .toArray(Object[]::new);
            return method.invoke(builder.getLastMatchedResource(), parameters);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException re) throw re;
            throw new RuntimeException(targetException);
        }
    }

    private static Optional<Object> injectContext(Parameter parameter, ResourceContext resourceContext, UriInfo uriInfo) {
        if (!parameter.isAnnotationPresent(Context.class)) return Optional.empty();
        if (parameter.getType().equals(ResourceContext.class)) return Optional.of(resourceContext);
        if (parameter.getType().equals(UriInfo.class)) return Optional.of(uriInfo);
        return Optional.of(resourceContext.getResource(parameter.getType()));
    }

    private static Optional<Object> injectParameter(Parameter parameter, UriInfo uriInfo) {
        return providers.stream()
                .map(provider -> provider.provide(parameter, uriInfo))
                .filter(Optional::isPresent)
                .findFirst()
                .flatMap(it -> it.flatMap(values -> convert(parameter, values)));
    }

    private static Optional<Object> convert(Parameter parameter, List<String> values) {
        return ConverterPrimitive.convert(parameter, values)
                .or(() -> ConverterConstructor.convert(parameter.getType(), values.get(0)))
                .or(() -> ConverterFactory.convert(parameter.getType(), values.get(0)))
                ;
    }

    interface ValueProvider {
        Optional<List<String>> provide(Parameter parameter, UriInfo uriInfo);
    }

    interface ValueConverter<T> {
        T fromString(List<String> values);

        static <T> ValueConverter<T> singleValued(Function<String, T> converter) {
            return list -> converter.apply(list.get(0));
        }
    }
}

class ConverterPrimitive {

    private static Map<Type, MethodInvoker.ValueConverter<Object>> primitives = Map.of(
            int.class, singleValued(Integer::parseInt),
            short.class, singleValued(Short::parseShort),
            float.class, singleValued(Float::parseFloat),
            double.class, singleValued(Double::parseDouble),
            byte.class, singleValued(Byte::parseByte),
            boolean.class, singleValued(Boolean::parseBoolean),
            String.class, singleValued(it -> it)
    );

    public static Optional<Object> convert(Parameter parameter, List<String> values) {
        return Optional.ofNullable(primitives.get(parameter.getType()))
                .map(c -> c.fromString(values));
    }
}

class ConverterConstructor {

    public static <T> Optional<T> convert(Class<T> converter, String value) {
        try {
            return Optional.of(converter.getConstructor(String.class).newInstance(value));
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException |
                 SecurityException | InvocationTargetException e) {
            return Optional.empty();
        }
    }
}

class ConverterFactory {
    public static Optional<?> convert(Class<?> converter, String value) {
        try {
            return Optional.of(converter.getMethod("valueOf", String.class).invoke(null, value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Optional.empty();
        }
    }
}