package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/22
 */
public class RootResourceTest {
    @Test
    void should_get_uri_template_from_path_annotation() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate uriTemplate = resource.getUriTemplate();

        assertTrue(uriTemplate.match("/messages/hello").isPresent());
    }


    @ParameterizedTest(name = "{3}")
    @CsvSource(textBlock = """
            GET,        /messages,          Messages.get,       Map to resource methods
            """
    )
    void should_match_resource_method_in_root_resource(String httpMethod, String path, String resourceMethod, String testName) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).get();
        assertEquals(resourceMethod, method.toString());
    }

    @Test
    void should_match_resource_method_in_sub_resource() {
        ResourceRouter.Resource resource = new SubResourceClass(new Message());
        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn("/content");

        assertTrue(resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).isPresent());
    }


    //TODO if sub resource Locator matches uri, using it to do follow up matching

    //TODO if no method / sub resource Locator matches, return 404
    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            GET,        /messages/hello,        No matched resource method
            """)
    void should_return_empty_if_not_matched_in_root_resource(String httpMethod, String uri, String testName) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(uri).get();
        assertTrue(resource.match(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).isEmpty());
    }

    //TODO if resource class does not have a path annotation, throw illegal argument
    //TODO Head and Option special case

    @Test
    @Disabled
    void should_add_last_match_resource_to_uri_info_builder() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match("/messages").get();
        StubUriInfoBuilder uriInfoBuilder = new StubUriInfoBuilder();
        resource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, uriInfoBuilder).get();

        assertTrue(uriInfoBuilder.getLastMatchedResource() instanceof Messages);
    }

    @Path("/messages")
    static class Messages {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String get() {
            return "messages";
        }

    }

    static class Message {
        @GET
        @Path("/content")
        @Produces(MediaType.TEXT_PLAIN)
        public String content() {
            return "content";
        }
    }

}
