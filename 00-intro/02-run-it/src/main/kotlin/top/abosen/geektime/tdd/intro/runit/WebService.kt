package top.abosen.geektime.tdd.intro.runit

import jakarta.ws.rs.core.UriBuilder
import org.glassfish.jersey.jetty.JettyHttpContainerFactory
import org.glassfish.jersey.server.ResourceConfig

class Application : ResourceConfig(StudentsResource::class.java)

fun main() {
    JettyHttpContainerFactory
        .createServer(UriBuilder.fromUri("http://localhost/").port(8051).build(), Application())
        .start()
}