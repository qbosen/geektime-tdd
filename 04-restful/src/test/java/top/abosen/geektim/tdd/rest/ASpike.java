package top.abosen.geektim.tdd.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
        handler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getWriter().write("test");
                resp.getWriter().flush();
            }
        }), "/");

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
        Assertions.assertEquals("test", response.body());
    }
}
