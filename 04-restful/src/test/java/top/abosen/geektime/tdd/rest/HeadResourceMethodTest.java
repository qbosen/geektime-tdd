package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ResourceContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/27
 */
public class HeadResourceMethodTest {

    @Test
    void should_call_method_and_ignore_return_value() {
        ResourceRouter.ResourceMethod resourceMethod = mock(ResourceRouter.ResourceMethod.class);
        ResourceContext resourceContext = mock(ResourceContext.class);
        UriInfoBuilder uriInfoBuilder = mock(UriInfoBuilder.class);

        HeadResourceMethod headResourceMethod = new HeadResourceMethod(resourceMethod);
        assertNull(headResourceMethod.call(resourceContext, uriInfoBuilder));

        Mockito.verify(resourceMethod).call(same(resourceContext), same(uriInfoBuilder));
    }

    @Test
    void should_delegate_to_method_for_uri_template() {
        ResourceRouter.ResourceMethod resourceMethod = mock(ResourceRouter.ResourceMethod.class);
        UriTemplate uriTemplate = mock(UriTemplate.class);
        HeadResourceMethod headResourceMethod = new HeadResourceMethod(resourceMethod);
        when(resourceMethod.getUriTemplate()).thenReturn(uriTemplate);
        assertSame(uriTemplate, headResourceMethod.getUriTemplate());
    }

    @Test
    void should_delegate_to_method_for_http_method() {
        ResourceRouter.ResourceMethod resourceMethod = mock(ResourceRouter.ResourceMethod.class);
        HeadResourceMethod headResourceMethod = new HeadResourceMethod(resourceMethod);
        when(resourceMethod.getHttpMethod()).thenReturn("GET");
        assertEquals(HttpMethod.HEAD, headResourceMethod.getHttpMethod());
    }
}
