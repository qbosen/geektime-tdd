package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/23
 */
public class SubResourceLocatorsTest {
    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,             Messages.hello,         fully match with URI
            /hello/content,     Messages.hello,         matched with URI
            /topics/1234,       Messages.message1234,   multiple matched choices
            """)
    void should_match_path_with_uri(String path, String resourceMethod, String testName) {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator locator = locators.findSubResource(path).get();
        assertEquals(resourceMethod, locator.toString());
    }

    @Test
    void should_return_empty_if_not_match_url() {
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        assertTrue(locators.findSubResource("/missing").isEmpty());
    }

    @Test
    void should_call_locator_method_to_generate_sub_resource() {
        StubUriInfoBuilder infoBuilder = new StubUriInfoBuilder();
        UriTemplate.MatchResult result = Mockito.mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn(null);

        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        ResourceRouter.SubResourceLocator subResourceLocator = locators.findSubResource("/hello").get();
        ResourceRouter.Resource subResource = subResourceLocator.getSubResource(infoBuilder);

        ResourceRouter.ResourceMethod method = subResource.match(result, "GET", new String[]{MediaType.TEXT_PLAIN}, infoBuilder).get();
        assertEquals("Message.content", method.toString());
        assertEquals("hello", ((Message) infoBuilder.getLastMatchedResource()).message);
    }

    @Path("/messages")
    static class Messages {
        @Path("/hello")
        public Message hello() {
            return new Message("hello");
        }

        @Path("/topics/{id}")
        public Message id() {
            return new Message("id");
        }

        @Path("/topics/1234")
        public Message message1234() {
            return new Message("message1234");
        }

    }

    static class Message {
        private String message;

        public Message(String message) {
            this.message = message;
        }

        @GET
        public String content() {
            return "content";
        }
    }

    class StubUriInfoBuilder implements UriInfoBuilder {
        private List<Object> matchedResource = new ArrayList<>();

        public StubUriInfoBuilder() {
            matchedResource.add(new Messages());
        }

        @Override
        public Object getLastMatchedResource() {
            return matchedResource.get(matchedResource.size()-1);
        }

        @Override
        public void addMatchedResource(Object resource) {
            matchedResource.add(resource);
        }
    }

}
