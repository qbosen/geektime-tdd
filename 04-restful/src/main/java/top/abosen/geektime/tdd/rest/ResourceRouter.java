package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder);
    }

    interface RootResource extends Resource, UriHandler {
    }

    interface ResourceMethod extends UriHandler {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);

        String getHttpMethod();
    }

    interface SubResourceLocator extends UriHandler {
        Resource getSubResource(ResourceContext resourceContext, UriInfoBuilder builder);
    }

}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<RootResource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();
        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);

        List<RootResource> rootResources = this.rootResources;

        return (OutboundResponse) UriHandlers.match(path, rootResources,
                        (result, handler) -> findResourceMethod(request, resourceContext, uri, result, handler))
                .map(m -> callMethod(resourceContext, uri, m)
                        .map(entity -> Response.ok(entity).build())
                        .orElseGet(() -> Response.noContent().build()))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build())
                ;
    }

    private static Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, ResourceContext resourceContext, UriInfoBuilder uri,
                                                               Optional<UriTemplate.MatchResult> matched, RootResource handler) {
        return matched.flatMap(it -> handler.match(it, request.getMethod(),
                Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), resourceContext, uri));
    }

    private static Optional<? extends GenericEntity<?>> callMethod(ResourceContext resourceContext, UriInfoBuilder uri, ResourceMethod m) {
        return Optional.ofNullable(m.call(resourceContext, uri));
    }

}

class RootResourceClass implements ResourceRouter.RootResource {

    private final Class<?> resourceClass;
    private final PathTemplate uriTemplate;
    private ResourceMethods resourceMethods;
    private final SubResourceLocators subResourceLocators;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathTemplate(resourceClass.getAnnotation(Path.class).value());
        this.resourceMethods = new ResourceMethods(resourceClass.getMethods());
        this.subResourceLocators = new SubResourceLocators(resourceClass.getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        Object resource = resourceContext.getResource(resourceClass);
        builder.addMatchedResource(resource);
        return resourceMethods.findResourceMethod(remaining, method).or(() ->
                subResourceLocators.findSubResourceMethod(remaining, method, mediaType, resourceContext, builder));
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }


}

class SubResourceClass implements ResourceRouter.Resource {
    private final ResourceMethods resourceMethods;
    private Object subResource;
    private SubResourceLocators subResourceLocators;

    public SubResourceClass(Object subResource) {
        this.subResource = subResource;
        this.resourceMethods = new ResourceMethods(subResource.getClass().getMethods());
        this.subResourceLocators = new SubResourceLocators(subResource.getClass().getMethods());
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(UriTemplate.MatchResult result, String method, String[] mediaType, ResourceContext resourceContext, UriInfoBuilder builder) {
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        return resourceMethods.findResourceMethod(remaining, method).or(() ->
                subResourceLocators.findSubResourceMethod(remaining, method, mediaType, resourceContext, builder));
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
        return UriHandlers.match(path, resourceMethods.getOrDefault(method, Collections.emptyList()), it -> it.getRemaining() == null);
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
                .map(a -> a.annotationType())
                .filter(a -> a.isAnnotationPresent(HttpMethod.class)).findFirst().map(it -> it.getAnnotation(HttpMethod.class).value()).get();
    }

    @Override
    public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
        return null;
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

class SubResourceLocators {

    private final List<ResourceRouter.SubResourceLocator> subResourceLocators;

    public SubResourceLocators(Method[] methods) {
        subResourceLocators = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Path.class) &&
                        Arrays.stream(method.getAnnotations()).noneMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(DefaultSubResourceLocator::new).collect(Collectors.toList());
    }

    public Optional<ResourceRouter.SubResourceLocator> findSubResource(String path) {
        return UriHandlers.match(path, subResourceLocators);
    }

    public Optional<ResourceRouter.ResourceMethod> findSubResourceMethod(String path, String method, String[] mediaTypes, ResourceContext resourceContext, UriInfoBuilder builder) {
        return UriHandlers.match(path, subResourceLocators, (result, locator) ->
                locator.getSubResource(resourceContext, builder).match(result.get(), method, mediaTypes, resourceContext, builder)
        );
    }

    static class DefaultSubResourceLocator implements ResourceRouter.SubResourceLocator {
        private final Method method;
        private final UriTemplate uriTemplate;

        public DefaultSubResourceLocator(Method method) {
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
        public ResourceRouter.Resource getSubResource(ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = builder.getLastMatchedResource();
            try {
                Object subResource = method.invoke(resource);
                builder.addMatchedResource(subResource);
                return new SubResourceClass(subResource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}