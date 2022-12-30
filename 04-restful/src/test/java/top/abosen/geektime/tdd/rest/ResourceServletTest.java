package top.abosen.geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
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
    private RuntimeDelegate delegate;


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
        delegate = mock(RuntimeDelegate.class);
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

    @Nested
    class RespondForOutboundResponse {
        @Test
        void should_use_http_headers_from_response() {
            response().headers(HttpHeaders.SET_COOKIE, new NewCookie.Builder("SESSION_ID").value("session").build(), new NewCookie.Builder("USER_ID").value("user").build())
                    .status(Response.Status.NOT_MODIFIED)
                    .returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");
            assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                    httpResponse.headers().allValues(HttpHeaders.SET_COOKIE).toArray(String[]::new));
        }

        @Test
        void should_write_entity_to_http_response_using_message_body_writer() {
            response().returnFrom(router);
            HttpResponse<String> httpResponse = get("/test");
            assertEquals("entity", httpResponse.body());
        }

        @Test
        void should_not_call_message_body_writer_if_entity_is_null() {
            response().entity(null, new Annotation[0]).returnFrom(router);
            HttpResponse<String> httpResponse = get("/test");
            assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
            assertEquals("", httpResponse.body());
        }

        @Test
        void should_use_status_from_response() {
            response().status(Response.Status.NOT_MODIFIED).returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");
            assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
        }
    }

    @TestFactory
    List<DynamicTest> RespondWhenExtensionMissing() {
        List<DynamicTest> tests = new ArrayList<>();
        // 这里的 entity, header, exception 都不是mock过的能处理的类型,所以在递归处理异常的时候 最后都会落到 null pointer
        Map<String, Executable> extensions = Map.of(
                "MessageBodyWriter", () -> response().entity(new GenericEntity<>(1, Integer.class), new Annotation[0]).returnFrom(router),
                "HeaderDelegate", () -> response().headers(HttpHeaders.DATE, new Date()).returnFrom(router),
                "ExceptionMapper", () -> when(router.dispatch(any(), eq(resourceContext))).thenThrow(IllegalStateException.class));
        for (String name : extensions.keySet())
            tests.add(DynamicTest.dynamicTest(name + " not found", () -> {
                extensions.get(name).execute();
                when(providers.getExceptionMapper(eq(NullPointerException.class))).thenReturn(e -> response().status(Response.Status.INTERNAL_SERVER_ERROR).build());
                HttpResponse<String> httpResponse = get("/test");
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), httpResponse.statusCode());
            }));
        return tests;
    }


    @TestFactory
    List<DynamicTest> RespondForException() {
        List<DynamicTest> tests = new ArrayList<>();

        Map<String, Consumer<Consumer<RuntimeException>>> exceptions = Map.of("Other exception", this::otherExceptionThrownFrom, "WebApplicationException", this::webApplicationExceptionThrownFrom);
        Map<String, Consumer<RuntimeException>> callers = getCallers();

        for (Map.Entry<String, Consumer<RuntimeException>> caller : callers.entrySet()) {
            for (Map.Entry<String, Consumer<Consumer<RuntimeException>>> exceptionThrownFrom : exceptions.entrySet()) {
                tests.add(DynamicTest.dynamicTest(caller.getKey() + " throws " + exceptionThrownFrom.getKey(),
                        () -> exceptionThrownFrom.getValue().accept(caller.getValue())));
            }
        }

        return tests;
    }

    private void otherExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new IllegalArgumentException();
        caller.accept(exception);
        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(e -> response().status(Response.Status.FORBIDDEN).build());
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private void webApplicationExceptionThrownFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());
        caller.accept(exception);
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ExceptionThrowsFrom {
    }

    @ExceptionThrowsFrom
    private void providers_getExceptionMapper(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenThrow(exception);
    }

    @ExceptionThrowsFrom
    private void runtimeDelegate_createHeaderDelegate(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(router);
        when(delegate.createHeaderDelegate(eq(MediaType.class))).thenThrow(exception);
    }

    @ExceptionThrowsFrom
    private void exceptionMapper_toResponse(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(e -> {
            throw exception;
        });
    }

    @ExceptionThrowsFrom
    private void headerDelegate_toString(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(router);
        when(delegate.createHeaderDelegate(eq(MediaType.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<MediaType>() {
            @Override
            public MediaType fromString(String value) {
                return null;
            }

            @Override
            public String toString(MediaType value) {
                throw exception;
            }
        });
    }

    @ExceptionThrowsFrom
    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(exception);
    }

    @ExceptionThrowsFrom
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

    @ExceptionThrowsFrom
    private void resourceRouter_dispatch(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
    }

    private Map<String, Consumer<RuntimeException>> getCallers() {
        Map<String, Consumer<RuntimeException>> callers = new HashMap<>();

        for (Method method : Arrays.stream(this.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ExceptionThrowsFrom.class)).toList()) {
            String name = method.getName();
            String callerName = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', '.');
            callers.put(callerName, e -> {
                try {
                    method.invoke(this, e);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        return callers;
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
            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType)))
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
