package top.abosen.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Consumer;

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

    }

    //DONE: use status code as http status
    @Test
    void should_use_status_from_response() throws Exception {
        response().status(Response.Status.NOT_MODIFIED).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    //DONE: use headers as http headers
    @Test
    void should_use_http_headers_from_response() throws Exception {
        response().headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build())
                .status(Response.Status.NOT_MODIFIED)
                .returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");
        assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
    }

    //DONE: writer body using MessageBodyWriter
    @Test
    void should_write_entity_to_http_response_using_message_body_writer() throws Exception {
        response().returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity", httpResponse.body());
    }

    //DONE: throw WebApplicationException with response, use response
    @Test
    void should_use_response_from_web_application_exception() throws Exception {
        response().status(Response.Status.FORBIDDEN)
                .headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("session").build())
                .entity(new GenericEntity<>("error", String.class), new Annotation[0])
                .throwFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        assertArrayEquals(new String[]{"SESSION_ID=session"},
                httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
        assertEquals("error", httpResponse.body());
    }

    //DONE: throw other exception, use ExceptionMapper build response
    @Test
    void should_build_response_by_exception_mapper_if_null_response_from_web_application_exception() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> response().status(Response.Status.FORBIDDEN).build());
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    //DONE: entity is null, ignore MessageBodyWriter
    @Test
    void should_not_call_message_body_writer_if_entity_is_null() throws Exception {
        response().entity(null, new Annotation[0]).returnFrom(router);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
        assertEquals("", httpResponse.body());
    }

    //TODO: 500 if MessageBodyWriter not found
    //TODO: 500 if header delegate
    //TODO: 500 if exception mapper
    //TODO exception mapper
    @Test
    void should_use_response_from_web_application_exception_thrown_by_exception_mapper() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> {
            throw new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());
        });

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_map_exception_thrown_by_exception_mapper() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(RuntimeException.class)).thenReturn(exception -> {
            throw new IllegalArgumentException();
        });
        when(providers.getExceptionMapper(IllegalArgumentException.class)).thenReturn(exception -> {
            throw new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());
        });

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_providers_when_find_message_body_writer() throws Exception {
        webApplicationExceptionThrownFrom(this::providers_getMessageBodyWriter);
    }

    @Test
    void should_use_response_from_web_application_exception_thrown_by_message_body_writer() throws Exception {
        webApplicationExceptionThrownFrom(this::messageBodyWriter_writeTo);
    }

    @Test
    void should_map_exception_thrown_by_providers_when_find_message_body_writer() throws Exception {
        otherExceptionThrownFrom(this::providers_getMessageBodyWriter);
    }

    @Test
    void should_map_exception_thrown_by_message_body_writer() throws Exception {
        otherExceptionThrownFrom(this::messageBodyWriter_writeTo);
    }

    private void otherExceptionThrownFrom(Consumer<RuntimeException> caller) throws Exception {
        RuntimeException exception = new IllegalArgumentException();
        caller.accept(exception);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(e -> response().status(Response.Status.FORBIDDEN).build());
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void webApplicationExceptionThrownFrom(Consumer<RuntimeException> caller) throws Exception {
        RuntimeException exception = new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());
        caller.accept(exception);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(exception);
    }

    private void messageBodyWriter_writeTo(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<Double>() {
                    @Override
                    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                        return false;
                    }

                    @Override
                    public void writeTo(Double aDouble, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                        throw exception;
                    }
                });
    }

    OutboundResponseBuilder response() {
        return new OutboundResponseBuilder();
    }

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

        void returnFrom(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        void throwFrom(ResourceRouter router) {
            build(response -> {
                WebApplicationException exception = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });
        }

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = build();
            consumer.accept(response);
        }

        private void stubMessageBodyWriter() {
            if (Objects.isNull(entity)) return;
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

        private OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            when(response.getHeaders()).thenReturn(headers);

            stubMessageBodyWriter();
            return response;
        }
    }
}
