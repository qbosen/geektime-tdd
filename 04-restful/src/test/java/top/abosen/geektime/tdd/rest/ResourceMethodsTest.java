package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/23
 */
public class ResourceMethodsTest {

    @ParameterizedTest(name = "{3}")
    @CsvSource(
            textBlock = """
                    GET,        /messages/hello,        Messages.hello,         GET and URI match
                    POST,       /messages/hello,        Messages.postHello,     POST and URI match
                    PUT,        /messages/hello,        Messages.putHello,      PUT and URI match
                    DELETE,     /messages/hello,        Messages.deleteHello,   DELETE and URI match
                    PATCH,      /messages/hello,        Messages.patchHello,    PATCH and URI match
                    HEAD,       /messages/hello,        Messages.headHello,     HEAD and URI match
                    OPTIONS,    /messages/hello,        Messages.optionsHello,  OPTIONS and URI match
                    GET,        /messages/topics/1234,  Messages.topic1234,     GET with multiply choice
                    GET,        /messages,              Messages.get,           GET and resource method without Path
                    HEAD,       /messages/head,         Messages.getHead,       HEAD with get resource method
                    """
    )
    void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String testName) {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match(path).get();
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethod(remaining, httpMethod).get();
        assertEquals(resourceMethod, method.toString());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /missing-messages/1,        URI not matched
            POST,       /missing-messages,          Http method not matched
            """)
    void should_return_empty_if_not_matched(String httpMethod, String uri, String testName) {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/missing-messages").match(uri).get();
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        assertTrue(resourceMethods.findResourceMethod(remaining, httpMethod).isEmpty());
    }

    @Test
    void should_convert_get_resource_method_to_head_resource_method() {
        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethod(remaining, "HEAD").get();

        assertInstanceOf(HeadResourceMethod.class, method);
    }

    @Test
    void should_get_options_for_given_uri() {
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);

        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext context = mock(ResourceContext.class);
        UriInfoBuilder builder = mock(UriInfoBuilder.class);

        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/head").get();
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethod(remaining, "OPTIONS").get();

        GenericEntity<?> entity = method.call(context, builder);
        Response response = (Response) entity.getEntity();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS), response.getAllowedMethods());
    }

    @Test
    void should_not_include_head_in_options_if_given_uri_not_have_get_method() {
        RuntimeDelegate delegate = Mockito.mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);

        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());
        ResourceContext context = mock(ResourceContext.class);
        UriInfoBuilder builder = mock(UriInfoBuilder.class);

        ResourceMethods resourceMethods = new ResourceMethods(Messages.class.getMethods());
        UriTemplate.MatchResult result = new PathTemplate("/messages").match("/messages/no-head").get();
        String remaining = Optional.ofNullable(result.getRemaining()).orElse("");
        ResourceRouter.ResourceMethod method = resourceMethods.findResourceMethod(remaining, "OPTIONS").get();

        GenericEntity<?> entity = method.call(context, builder);
        Response response = (Response) entity.getEntity();

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(Set.of(HttpMethod.POST, HttpMethod.OPTIONS), response.getAllowedMethods());
    }


    @Path("/missing-messages")
    static class MissingMessages {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }

    }

    @Path("/messages")
    static class Messages {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }

        @GET
        @Path("/head")
        @Produces(MediaType.TEXT_PLAIN)
        public String getHead() {
            return "head";
        }

        @POST
        @Path("/no-head")
        @Produces(MediaType.TEXT_PLAIN)
        public void postNoHead() {
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @PUT
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String putHello() {
            return "hello";
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello() {
            return "hello";
        }

        @DELETE
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String deleteHello() {
            return "hello";
        }

        @PATCH
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String patchHello() {
            return "hello";
        }

        @HEAD
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String headHello() {
            return "hello";
        }

        @OPTIONS
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String optionsHello() {
            return "hello";
        }

        @GET
        @Path("/topics/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String topicId() {
            return "topicId";
        }

        @GET
        @Path("/topics/1234")
        @Produces(MediaType.TEXT_PLAIN)
        public String topic1234() {
            return "topic1234";
        }
    }
}
