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

    @BeforeEach
    void before() {
        delegate = mock(RuntimeDelegate.class);
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

        Router router = new Router(Users.class);

        OutboundResponse response = router.dispatch(request, context);
        GenericEntity<String> entity = response.getGenericEntity();
        assertEquals("all", entity.getEntity());
    }

    static class Router implements ResourceRouter {
        private Map<Pattern, Class<?>> routerTable = new HashMap<>();

        public Router(Class<Users> rootResource) {
            Path path = rootResource.getAnnotation(Path.class);
            routerTable.put(Pattern.compile(path.value() + "(/.*)?"), rootResource);
        }

        @Override
        public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
            String path = request.getServletPath();

            Pattern matched = routerTable.keySet().stream().filter(pattern -> pattern.matcher(path).matches()).findFirst().get();
            Class<?> resource = routerTable.get(matched);

            Method method = Arrays.stream(resource.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
            Object object = resourceContext.getResource(resource);
            try {
                Object result = method.invoke(object);
                GenericEntity entity = new GenericEntity(result, method.getGenericReturnType());
                return (OutboundResponse) Response.ok(entity).build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Path("/users")
    static class Users {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String asText() {
            return "all";
        }

        @GET
        @Produces(MediaType.TEXT_HTML)
        public String asHtml() {
            return "all";
        }
    }

}
