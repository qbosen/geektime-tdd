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
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
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
    private OutboundResponseBuilder builder;


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
        builder = new OutboundResponseBuilder();

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

    }

    //DONE: use status code as http status
    @Test
    void should_use_status_from_response() throws Exception {
        builder.status(Response.Status.NOT_MODIFIED).build();

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    //DONE: use headers as http headers
    @Test
    void should_use_http_headers_from_response() throws Exception {
        builder.headers("Set-Cookie", new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build())
                .status(Response.Status.NOT_MODIFIED)
                .build();

        HttpResponse<String> httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    //DONE: writer body using MessageBodyWriter
    @Test
    void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        builder.build();
        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }
//TODO: 500 if MessageBodyWriter not found
//TODO: throw WebApplicationException with response, use response
//TODO: throw WebApplicationException with null response, use ExceptionMapper build response
//TODO: throw other exception, use ExceptionMapper build response

    class OutboundResponseBuilder {
        private Response.Status status = Response.Status.OK;
        private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        private Annotation[] annotations = new Annotation[0];
        private MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutboundResponseBuilder headers(String name, Object... values) {
            headers.addAll(name, values);
            return this;
        }

        public OutboundResponseBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        public OutboundResponseBuilder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        void build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            when(response.getHeaders()).thenReturn(headers);
            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

            Mockito.<MessageBodyWriter<?>>when(providers.getMessageBodyWriter(eq(entity.getRawType()), eq(entity.getType()), same(annotations), eq(mediaType)))
                    .thenReturn(new MessageBodyWriter<String>() {
                        @Override
                        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations1, MediaType mediaType1) {
                            return false;
                        }

                        @Override
                        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations1, MediaType mediaType1, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                            PrintWriter writer = new PrintWriter(entityStream);
                            writer.write(s);
                            writer.flush();
                        }
                    });

        }
    }
}
