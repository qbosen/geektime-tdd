package top.abosen.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
public class ResourceServletTest extends ServletTest {
    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        return new ResourceServlet(runtime);
    }

    //DONE: use status code as http status
    @Test
    void should_use_status_from_response() throws Exception {
        OutboundResponse response = mock(OutboundResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    //DONE: use headers as http headers
    @Test
    void should_use_http_headers_from_response() throws Exception {
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createHeaderDelegate(eq(NewCookie.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<NewCookie>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return value.getName() + "=" + value.getValue();
            }
        });

        OutboundResponse response = mock(OutboundResponse.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie",
                new NewCookie.Builder("SESSION_ID").value("session").build(),
                new NewCookie.Builder("USER_ID").value("user").build()
        );
        when(response.getHeaders()).thenReturn(headers);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
        when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());


        HttpResponse<String> httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }
//TODO: writer body using MessageBodyWriter
//TODO: 500 if MessageBodyWriter not found
//TODO: throw WebApplicationException with response, use response
//TODO: throw WebApplicationException with null response, use ExceptionMapper build response
//TODO: throw other exception, use ExceptionMapper build response

}
