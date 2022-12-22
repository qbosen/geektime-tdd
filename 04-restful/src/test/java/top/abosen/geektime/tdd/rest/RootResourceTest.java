package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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


    @ParameterizedTest
    @CsvSource({"GET,/messages/hello,Messages.hello", "GET,/messages/ah,Messages.ah", "POST,/messages/hello,Messages.postHello"})
    void should_match_resource_method(String httpMethod, String path, String resourceMethod) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        ResourceRouter.ResourceMethod method = resource.match(path, httpMethod, new String[]{MediaType.TEXT_PLAIN}, Mockito.mock(UriInfoBuilder.class)).get();
        assertEquals(resourceMethod, method.toString());
    }


    //TODO if sub resource Locator matches uri, using it to do follow up matching
    //TODO if no method / sub resource Locator matches, return 404
    //TODO if resource class does not have a path annotation, throw illegal argument

    @Path("/messages")
    static class Messages {
        @GET
        @Path("/ah")
        @Produces(MediaType.TEXT_PLAIN)
        public String ah() {
            return "ah";
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello() {
            return "hello";
        }
    }

}
