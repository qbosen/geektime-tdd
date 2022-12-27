package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/22
 */
public class RootResourceTest {
    private ResourceContext context;
    private Messages rootResource;

    @BeforeEach
    public void setup() {
        rootResource = new Messages();
        context = mock(ResourceContext.class);
        when(context.getResource(eq(Messages.class))).thenReturn(rootResource);
    }

    @Test
    void should_get_uri_template_from_path_annotation() {
        ResourceRouter.Resource resource = new ResourceHandler(Messages.class);
        UriTemplate uriTemplate = resource.getUriTemplate();

        assertTrue(uriTemplate.match("/messages/hello").isPresent());
    }


    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,        /messages,              Messages.get,           Map to resource method
            GET,        /messages/1/content,    Message.content,        Map to sub-resource method
            GET,        /messages/1/body,       MessageBody.get,        Map to sub-sub-resource method
            """
    )
    void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String testName) {
        UriInfoBuilder builder = new StubUriInfoBuilder();
        ResourceRouter.Resource resource = new ResourceHandler(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, context, builder).get();
        assertEquals(resourceMethod, method.toString());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /messages/hello,        No matched resource method
            GET,        /messages/1/header,     No matched sub-resource method
            """)
    void should_return_empty_if_not_matched_in_root_resource(String httpMethod, String uri, String testName) {
        UriInfoBuilder builder = new StubUriInfoBuilder();
        ResourceRouter.Resource resource = new ResourceHandler(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(uri).get();
        assertTrue(resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, context, builder).isEmpty());
    }

    @Test
    void should_throw_illegal_argument_exception_if_root_resource_not_have_path_annotation() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceHandler(Message.class));
    }

    @Test
    void should_add_last_match_resource_to_uri_info_builder() {
        ResourceRouter.Resource resource = new ResourceHandler(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match("/messages").get();
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, context, uriInfoBuilder).get();

        assertTrue(uriInfoBuilder.getLastMatchedResource() instanceof Messages);
    }

    @Path("/messages")
    static class Messages {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }


        @Path("/{id:[0-9]+}")
        public Message getById() {
            return new Message();
        }
    }

    static class Message {
        @GET
        @Path("/content")
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }

        @Path("/body")
        public MessageBody messageBody(){
            return new MessageBody();
        }
    }

    static class MessageBody {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messageBody";
        }
    }
}
