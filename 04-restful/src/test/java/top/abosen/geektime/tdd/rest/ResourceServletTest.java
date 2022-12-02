package top.abosen.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
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
    private Providers providers;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);
        return new ResourceServlet(runtime);
    }

    @BeforeEach
    void before() {
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


        when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<String>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        PrintWriter writer = new PrintWriter(entityStream);
                        writer.write(s);
                        writer.flush();
                    }
                });

    }

    //DONE: use status code as http status
    @Test
    void should_use_status_from_response() throws Exception {
        response(Response.Status.NOT_MODIFIED, new MultivaluedHashMap<>(), new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    //DONE: use headers as http headers
    @Test
    void should_use_http_headers_from_response() throws Exception {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie",
                new NewCookie.Builder("SESSION_ID").value("session").build(),
                new NewCookie.Builder("USER_ID").value("user").build()
        );
        response(Response.Status.NOT_MODIFIED, headers, new GenericEntity<>("entity", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);


        HttpResponse<String> httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    //DONE: writer body using MessageBodyWriter
    @Test
    void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        response(Response.Status.OK, new MultivaluedHashMap<>(), entity, annotations, mediaType);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }
//TODO: 500 if MessageBodyWriter not found
//TODO: throw WebApplicationException with response, use response
//TODO: throw WebApplicationException with null response, use ExceptionMapper build response
//TODO: throw other exception, use ExceptionMapper build response

    private void response(Response.Status status, MultivaluedMap<String, Object> headers, GenericEntity<Object> entity, Annotation[] annotations, MediaType mediaType) {
        OutboundResponse response = mock(OutboundResponse.class);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        when(response.getGenericEntity()).thenReturn(entity);
        when(response.getAnnotations()).thenReturn(annotations);
        when(response.getMediaType()).thenReturn(mediaType);
        when(response.getHeaders()).thenReturn(headers);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);
    }
}
