package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static top.abosen.geektime.tdd.rest.DefaultResourceMethod.ValueConverter.singleValued;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource extends UriHandler {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface ResourceMethod extends UriHandler {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);

        String getHttpMethod();
    }

}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<Resource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<Resource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);

        List<Resource> rootResources = this.rootResources;

        return (OutboundResponse) UriHandlers.match(path, rootResources,
                        (result, handler) -> findResourceMethod(request, resourceContext, uri, result, handler))
                .map(m -> callMethod(resourceContext, uri, m)
                        .map(entity -> {
                            if (entity.getEntity() instanceof OutboundResponse) {
                                return ((OutboundResponse) entity.getEntity());
                            }
                            return Response.ok(entity).build();
                        })
                        .orElseGet(() -> Response.noContent().build()))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build())
                ;
    }

    private static Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, ResourceContext resourceContext, UriInfoBuilder uri,
                                                               Optional<UriTemplate.MatchResult> matched, Resource handler) {
        return matched.flatMap(it -> handler.match(it, request.getMethod(),
                Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), resourceContext, uri));
    }

    private static Optional<? extends GenericEntity<?>> callMethod(ResourceContext resourceContext, UriInfoBuilder uri, ResourceMethod m) {
        return Optional.ofNullable(m.call(resourceContext, uri));
    }

}


class ResourceMethods {
    private final Map<String, List<ResourceRouter.ResourceMethod>> resourceMethods;

    public ResourceMethods(Method[] methods) {
        this.resourceMethods = getResourceMethods(methods);
    }

    private static Map<String, List<ResourceRouter.ResourceMethod>> getResourceMethods(Method[] methods) {
        return Arrays.stream(methods).filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(it -> it.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(DefaultResourceMethod::new)
                .collect(Collectors.groupingBy(ResourceRouter.ResourceMethod::getHttpMethod));
    }

    public Optional<ResourceRouter.ResourceMethod> findResourceMethod(String path, String method) {
        return findMethods(path, method).or(() -> findAlternative(path, method));
    }

    private Optional<ResourceRouter.ResourceMethod> findAlternative(String remaining, String httpMethod) {
        if (HttpMethod.HEAD.equals(httpMethod))
            return findResourceMethod(remaining, HttpMethod.GET).map(HeadResourceMethod::new);
        if (HttpMethod.OPTIONS.equals(httpMethod)) {
            return Optional.of(new OptionResourceMethod(remaining));
        }
        return Optional.empty();
    }


    private Optional<ResourceRouter.ResourceMethod> findMethods(String path, String method) {
        return UriHandlers.match(path, resourceMethods.getOrDefault(method, Collections.emptyList()), it -> it.getRemaining() == null);
    }

    class OptionResourceMethod implements ResourceRouter.ResourceMethod {

        private final String path;

        public OptionResourceMethod(String path) {
            this.path = path;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return new GenericEntity<>(Response.noContent().allow(findAllowedMethod()).build(), Response.class);
        }

        private Set<String> findAllowedMethod() {
            Set<String> allowed = Stream.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS,
                            HttpMethod.PUT, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PATCH)
                    .filter(method -> findMethods(path, method).isPresent())
                    .collect(Collectors.toSet());
            allowed.add(HttpMethod.OPTIONS);
            if (allowed.contains(HttpMethod.GET)) allowed.add(HttpMethod.HEAD);

            return allowed;
        }

        @Override
        public String getHttpMethod() {
            return null;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return null;
        }
    }
}

class DefaultResourceMethod implements ResourceRouter.ResourceMethod {

    private final Method method;
    private final UriTemplate uriTemplate;
    private final String httpMethod;

    public DefaultResourceMethod(Method method) {
        this.method = method;
        this.uriTemplate = new PathTemplate(Optional.ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse(""));
        this.httpMethod = Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(a -> a.isAnnotationPresent(HttpMethod.class)).findFirst().map(it -> it.getAnnotation(HttpMethod.class).value()).get();
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        try {
            UriInfo uriInfo = builder.createUriInfo();
            Object[] parameters = Arrays.stream(method.getParameters()).map(parameter ->
                    providers.stream()
                            .map(provider -> provider.provide(parameter, uriInfo))
                            .filter(Optional::isPresent)
                            .findFirst()
                            .flatMap(it -> it.flatMap(values -> convert(parameter, values)))
                            .orElse(null)
            ).toArray(Object[]::new);
            Object result = method.invoke(builder.getLastMatchedResource(), parameters);
            return result == null ? null : new GenericEntity<>(result, method.getGenericReturnType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Object> convert(Parameter parameter, List<String> values) {
        return ConverterPrimitive.convert(parameter, values)
                .or(() -> ConverterConstructor.convert(parameter.getType(), values.get(0)))
                .or(() -> ConverterFactory.convert(parameter.getType(), values.get(0)))
                ;
    }

    private static ValueProvider pathParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(PathParam.class))
            .map(annotation -> uriInfo.getPathParameters().get(annotation.value()));
    private static ValueProvider queryParam = (parameter, uriInfo) -> Optional.ofNullable(parameter.getAnnotation(QueryParam.class))
            .map(annotation -> uriInfo.getQueryParameters().get(annotation.value()));
    private static final List<ValueProvider> providers = List.of(pathParam, queryParam);

    interface ValueProvider {
        Optional<List<String>> provide(Parameter parameter, UriInfo uriInfo);
    }

    interface ValueConverter<T> {
        T fromString(List<String> values);

        static <T> ValueConverter<T> singleValued(Function<String, T> converter) {
            return list -> converter.apply(list.get(0));
        }
    }


    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String toString() {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}

class HeadResourceMethod implements ResourceRouter.ResourceMethod {
    private final ResourceRouter.ResourceMethod target;

    HeadResourceMethod(ResourceRouter.ResourceMethod target) {
        this.target = target;
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        return target.call(resourceContext, builder);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.HEAD;
    }

    @Override
    public UriTemplate getUriTemplate() {
        return target.getUriTemplate();
    }

    @Override
    public String toString() {
        return target.toString();
    }
}

class SubResourceLocators {

    private final List<ResourceRouter.Resource> rootResources;

    public SubResourceLocators(Method[] methods) {
        rootResources = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Path.class) &&
                        Arrays.stream(method.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(SubResourceLocator::new).collect(Collectors.toList());
    }

    public Optional<ResourceRouter.ResourceMethod> findSubResourceMethod(String path, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        return UriHandlers.match(path, rootResources, (result, locator) ->
                locator.match(result.get(), method, mediaTypes, resourceContext, builder)
        );
    }

    static class SubResourceLocator implements ResourceRouter.Resource {
        private final Method method;
        private final UriTemplate uriTemplate;

        public SubResourceLocator(Method method) {
            this.method = method;
            uriTemplate = new PathTemplate(method.getAnnotation(Path.class).value());
        }

        @Override
        public UriTemplate getUriTemplate() {
            return uriTemplate;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }

        @Override
        public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = builder.getLastMatchedResource();
            try {
                Object subResource = method.invoke(resource);
                return new ResourceHandler(subResource, uriTemplate).match(result, httpMethod, mediaType, resourceContext, builder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}

class ResourceHandler implements ResourceRouter.Resource {

    private final UriTemplate uriTemplate;
    private final ResourceMethods resourceMethods;
    private final SubResourceLocators subResourceLocators;
    private final Function<ResourceContext, Object> resource;

    public ResourceHandler(Class<?> resourceClass) {
        this(resourceClass, new PathTemplate(getTemplate(resourceClass)), rc -> rc.getResource(resourceClass));
    }

    private static String getTemplate(Class<?> resourceClass) {
        if (!resourceClass.isAnnotationPresent(Path.class)) throw new IllegalArgumentException();
        return resourceClass.getAnnotation(Path.class).value();
    }

    public ResourceHandler(Object resource, UriTemplate uriTemplate) {
        this(resource.getClass(), uriTemplate, rc -> resource);
    }

    private ResourceHandler(Class<?> resourceClass, UriTemplate uriTemplate, Function<ResourceContext, Object> resource) {
        this.uriTemplate = uriTemplate;
        this.resource = resource;
        this.resourceMethods = new ResourceMethods(resourceClass.getMethods());
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String httpMethod, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        builder.addMatchedResource(resource.apply(resourceContext));
        return resourceMethods.findResourceMethod(remaining, httpMethod)
                .or(() -> subResourceLocators.findSubResourceMethod(remaining, httpMethod, mediaType, resourceContext, builder));
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

}

class ConverterPrimitive {

    private static Map<Type, DefaultResourceMethod.ValueConverter<Object>> primitives = Map.of(
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