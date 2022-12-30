package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/23
 */
public class SubResourceLocatorsTest {
    @ParameterizedTest(name = "{2}")
    @CsvSource(textBlock = """
            /hello,             hello,          fully match with URI
            /topics/1234,       1234,           multiple matched choices
            /topics/1,          id,             matched with variable
            """)
    void should_match_path_with_uri(String path, String message, String testName) {
        UriInfoBuilder infoBuilder = new StubUriInfoBuilder();
        infoBuilder.addMatchedResource(new Messages());
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        Optional<ResourceRouter.ResourceMethod> method = locators.findSubResourceMethod(
                path, "GET", new String[]{MediaType.TEXT_PLAIN}, mock(ResourceContext.class), infoBuilder);
        assertTrue(method.isPresent());

        assertEquals(message, ((Message) infoBuilder.getLastMatchedResource()).message);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(textBlock = """
            /missing,               unmatched resource method
            /hello/content,         unmatched sub-resource method
            """)
    void should_return_empty_if_not_match_url(String path, String testName) {
        UriInfoBuilder infoBuilder = new StubUriInfoBuilder();
        infoBuilder.addMatchedResource(new Messages());
        SubResourceLocators locators = new SubResourceLocators(Messages.class.getMethods());
        Optional<ResourceRouter.ResourceMethod> method = locators.findSubResourceMethod(
                path, "GET", new String[]{MediaType.TEXT_PLAIN}, mock(ResourceContext.class), infoBuilder);
        assertTrue(method.isEmpty());
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
            return new Message("1234");
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

}
