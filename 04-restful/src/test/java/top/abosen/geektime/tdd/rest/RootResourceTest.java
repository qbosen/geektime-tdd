package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    //TODO find resource method, matches the http request and http method
    @Test
    void should_match_resource_method_if_uri_and_http_method_fully_matched() {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        ResourceRouter.ResourceMethod method = resource.match("/messages/hello", "GET", new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).get();

        assertEquals("Messages.hello", method.toString());
    }


    //TODO if sub resource Locator matches uri, using it to do follow up matching
    //TODO if no method / sub resource Locator matches, return 404
    //TODO if resource class does not have a path annotation, throw illegal argument

    @Path("/messages")
    static class Messages {
        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }
    }

}
