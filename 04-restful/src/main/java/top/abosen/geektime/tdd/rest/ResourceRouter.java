package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(String path, String method, String[] mediaType, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
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

        return (OutboundResponse) rootResources.stream().map(resource -> match(path, resource))
                .filter(Result::isMatched).sorted().findFirst()
                .flatMap(result -> result.findResourceMethod(request, uri))
                .map(m -> callMethod(resourceContext, uri, m)
                        .map(entity -> Response.ok(entity).build())
                        .orElseGet(() -> Response.noContent().build()))
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build())
                ;
    }

    private static Optional<? extends GenericEntity<?>> callMethod(ResourceContext resourceContext, UriInfoBuilder uri, ResourceMethod m) {
        return Optional.ofNullable(m.call(resourceContext, uri));
    }

    private static Result match(String path, RootResource it) {
        return new Result(it.getUriTemplate().match(path), it);
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) implements Comparable<Result> {
        private Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, UriInfoBuilder uri) {
            return matched.flatMap(it -> resource.match(it.getRemaining(), request.getMethod(),
                    Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri));
        }

        public boolean isMatched() {
            return matched.isPresent();
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }

}

class RootResourceClass implements ResourceRouter.RootResource {

    private final Class<?> resourceClass;
    private final PathTemplate uriTemplate;
    private final List<ResourceRouter.ResourceMethod> resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        uriTemplate = new PathTemplate(resourceClass.getAnnotation(Path.class).value());
        resourceMethods = Arrays.stream(resourceClass.getMethods()).filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(it -> it.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> new DefaultResourceMethod(m))
                .collect(Collectors.toList());

    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> match(String path, String method, String[] mediaType, UriInfoBuilder builder) {
        return resourceMethods.stream().findFirst();
    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    static class DefaultResourceMethod implements ResourceRouter.ResourceMethod {

        private final Method method;

        public DefaultResourceMethod(Method method) {
            this.method = method;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return null;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
    }
}
