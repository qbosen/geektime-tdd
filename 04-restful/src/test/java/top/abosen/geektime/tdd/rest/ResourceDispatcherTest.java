package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/15
 */
public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;
    private Runtime runtime;

    @BeforeEach
    void before() {
        delegate = mock(RuntimeDelegate.class);
        runtime = mock(Runtime.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new Response.ResponseBuilder() {

            private Object entity;
            private int status;

            @Override
            public Response build() {
                OutboundResponse response = mock(OutboundResponse.class);
                when(response.getGenericEntity()).thenReturn((GenericEntity) entity);
                when(response.getStatus()).thenReturn(status);
                return response;
            }

            @Override
            public Response.ResponseBuilder clone() {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status) {
                return null;
            }

            @Override
            public Response.ResponseBuilder status(int status, String reasonPhrase) {
                this.status = status;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity) {
                this.entity = entity;
                return this;
            }

            @Override
            public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(String... methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder allow(Set<String> methods) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
                return null;
            }

            @Override
            public Response.ResponseBuilder encoding(String encoding) {
                return null;
            }

            @Override
            public Response.ResponseBuilder header(String name, Object value) {
                return null;
            }

            @Override
            public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(String language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder language(Locale language) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(MediaType type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder type(String type) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variant(Variant variant) {
                return null;
            }

            @Override
            public Response.ResponseBuilder contentLocation(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder cookie(NewCookie... cookies) {
                return null;
            }

            @Override
            public Response.ResponseBuilder expires(Date expires) {
                return null;
            }

            @Override
            public Response.ResponseBuilder lastModified(Date lastModified) {
                return null;
            }

            @Override
            public Response.ResponseBuilder location(URI location) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(EntityTag tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder tag(String tag) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(Variant... variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder variants(List<Variant> variants) {
                return null;
            }

            @Override
            public Response.ResponseBuilder links(Link... links) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(URI uri, String rel) {
                return null;
            }

            @Override
            public Response.ResponseBuilder link(String uri, String rel) {
                return null;
            }
        });
    }

    @Test
    void should() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ResourceContext context = Mockito.mock(ResourceContext.class);

        when(request.getServletPath()).thenReturn("/users");
        when(context.getResource(eq(Users.class))).thenReturn(new Users());

        Router router = new Router(runtime, List.of(new ResourceClass(Users.class)));

        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = response.getGenericEntity();
        assertEquals("all", entity.getEntity());
    }

    interface UriTemplate {
        @Path(("/{id}")) // /1/orders
        interface MatchResult extends Comparable<MatchResult> {
            String getMatchedPath();    // /1

            String getRemaining(); // orders

            Map<String, String> getMatchedPathParameters();     //{id:1}
        }

        Optional<MatchResult> match(String path);
    }

    static class Router implements ResourceRouter {

        private final List<Resource> rootResource;
        private Runtime runtime;

        public Router(Runtime runtime, List<Resource> rootResources) {
            this.runtime = runtime;
            this.rootResource = rootResources;
        }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
//            UriInfoBuilder builder = runtime.createUriInfoBuilder();
            ResourceMethod resourceMethod = rootResource.stream().map(root -> root.matches(request.getServletPath(), "GET", new String[0], null))
                    .filter(it -> it.isPresent()).findFirst().get().get();

            try {
                GenericEntity entity = resourceMethod.call(resourceContext, null);
                return (OutboundResponse) Response.ok(entity).build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ResourceClass implements RootResource {
        private final String path;
        private final Pattern pattern;
        private final Class<?> resourceClass;
        private final Map<UriTemplate, ResourceMethod> methods = new HashMap<>();


        record UriTemplate(Pattern uri, String[] mediaType) {

        }

        @Override
        public ResourceDispatcherTest.UriTemplate getUriTemplate() {
            return null;
        }

        public ResourceClass(Class<?> resourceClass) {
            this.resourceClass = resourceClass;
            this.path = resourceClass.getAnnotation(Path.class).value();
            this.pattern = Pattern.compile(path + "(/.*)?");
            Arrays.stream(resourceClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).forEach(method ->
                    methods.put(new UriTemplate(pattern, Optional.ofNullable(method.getAnnotation(Produces.class))
                                    .map(Produces::value).orElseGet(() -> new String[0])),
                            new NormalResourceMethod(resourceClass, method))
            );

            Arrays.stream(resourceClass.getMethods()).filter(m -> m.isAnnotationPresent(Path.class)).forEach(method ->
                    {
                        Path subPath = method.getAnnotation(Path.class);
                        Pattern pattern = Pattern.compile(this.path + ("(/" + subPath + ")?"));
                        methods.put(new UriTemplate(pattern, Optional.ofNullable(method.getAnnotation(Produces.class))
                                        .map(Produces::value).orElseGet(() -> new String[0])),
                                new SubResourceLocator(resourceClass, method, new String[0]));
                    }
            );


        }

        @Override
        public Optional<ResourceMethod> matches(String path, String method, String[] mediaType, UriInfoBuilder builder) {
            if (!pattern.matcher(path).matches()) return Optional.empty();
            return methods.entrySet().stream().filter(e -> e.getKey().uri.matcher(path).matches())
                    .map(e -> e.getValue()).findFirst();
        }
    }

    static class NormalResourceMethod implements ResourceMethod {

        private final Class<?> resourceClass;
        private final Method method;

        public NormalResourceMethod(Class<?> resourceClass, Method method) {
            this.resourceClass = resourceClass;
            this.method = method;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = resourceContext.getResource(resourceClass);
            try {
                return new GenericEntity(method.invoke(resource), method.getGenericReturnType());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class SubResourceLocator implements ResourceMethod {
        private Class<?> resourceClass;
        private Method method;
        private String[] mediaTypes;

        public SubResourceLocator(Class<?> resourceClass, Method method, String[] mediaTypes) {
            this.resourceClass = resourceClass;
            this.method = method;
            this.mediaTypes = mediaTypes;
        }

        @Override
        public GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder) {
            Object resource = resourceContext.getResource(resourceClass);
            try {
                Object subResource = method.invoke(resource);

                return new SubResource(subResource).matches(builder.getUnmatchedPath(), "GET", mediaTypes, builder)
                        .get().call(resourceContext, builder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class SubResource implements Resource {

        private final Class<?> subResourceClass;
        private Object subResource;

        public SubResource(Object subResource) {
            this.subResource = subResource;
            subResourceClass = subResource.getClass();
        }

        @Override
        public Optional<ResourceMethod> matches(String path, String method, String[] mediaType, UriInfoBuilder builder) {
            return Optional.empty();
        }
    }


    interface Resource {
        Optional<ResourceMethod> matches(String path, String method, String[] mediaType, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }

    public interface UriInfoBuilder {
        void pushMatchedPath(String path);

        void addParameter(String name, String value);

        String getUnmatchedPath();
    }

    @Path("/users")
    static class Users {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String asText() {
            return "all";
        }

        @Path("/orders")
        public Orders getOrders() {
            return new Orders();
        }
    }

    static class Orders {
        @GET
        public String asText() {
            return "all";
        }
    }

}
