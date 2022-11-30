package top.abosen.geektim.tdd.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.abosen.geektime.tdd.di.ComponentRef;
import top.abosen.geektime.tdd.di.Config;
import top.abosen.geektime.tdd.di.Context;
import top.abosen.geektime.tdd.di.ContextConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author qiubaisen
 * @date 2022/11/30
 */
public class ASpike {
    Server server;

    @BeforeEach
    void start() throws Exception {
        server = new Server(8080);
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        TestApplication application = new TestApplication();
        handler.addServlet(new ServletHolder(new ResourceServlet(application, new TestProviders(application))), "/");

        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.stop();
    }

    @Test
    void should() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/")).GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
        Assertions.assertEquals("prefixprefixtest", response.body());
    }

    static class ResourceServlet extends HttpServlet {
        private final Context context;
        private TestApplication application;
        private Providers providers;

        public ResourceServlet(TestApplication application, Providers providers) {
            this.application = application;
            this.providers = providers;
            context = application.getContext();
        }


        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Stream<Class<?>> rootResources = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));

            Object result = dispatch(req, rootResources);

            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(result.getClass(), null, null, null);
            writer.writeTo(result, null, null, null, null, null, resp.getOutputStream());
        }

        //request scope
        Object dispatch(HttpServletRequest req, Stream<Class<?>> rootResources) {
            try {
                Class<?> rootClass = rootResources.findFirst().get();
                Object rootResource = context.get(ComponentRef.of(rootClass));
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();

                return method.invoke(rootResource);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    //application scope
    static class TestProviders implements Providers {
        private final List<MessageBodyWriter> writers;

        private TestApplication application;

        public TestProviders(TestApplication application) {
            this.application = application;
            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            Context context = application.getContext();
            writers = (List<MessageBodyWriter>) writerClasses.stream().map(c -> context.get(ComponentRef.of(c))).toList();
        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return (MessageBodyWriter<T>) writers.stream().filter(w -> w.isWriteable(type, genericType, annotations, mediaType))
                    .findFirst().get();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            return null;
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            return null;
        }
    }

    static class StringMessageBodyWriter implements MessageBodyWriter<String> {
        @Inject
        @Named("prefix")
        String prefix;

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            PrintWriter writer = new PrintWriter(entityStream);
            writer.write(prefix);
            writer.write(s);
            writer.flush();
        }
    }

    static class TestApplication extends Application {
        Context context;
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }

        public Config getConfig() {
            return new Config() {
                @Named("prefix")
                public String name = "prefix";
            };
        }

        public Context getContext(){
            return context;
        }

        public TestApplication() {
            ContextConfig config = new ContextConfig();
            config.from(getConfig());

            List<Class<?>> writerClasses = this.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                config.bindComponent(writerClass, writerClass);
            }

            List<Class<?>> rootResources = this.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class)).toList();
            for (Class rootResource : rootResources) {
                config.bindComponent(rootResource, rootResource);
            }

            context = config.getContext();
        }
    }

    @Path("/test")
    static class TestResource {
        @Inject
        @Named("prefix")
        String prefix;

        @GET
        public String get() {
            return prefix + "test";
        }
    }

}
