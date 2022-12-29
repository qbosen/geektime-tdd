package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * @author qiubaisen
 * @date 2022/12/29
 */
class SubResourceLocatorTest extends InjectableCallerTest{


    @Override
    protected Object initResource() {
        return  Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    lastCall = new LastCall(
                            getMethodName(name, method.getParameterTypes()),
                            args == null ? List.of() : List.of(args));
                    return new Message();
                });
    }



    @Test
    void should_inject_string_path_param_to_sub_resource_method() throws NoSuchMethodException {
        parameters.put("param", List.of("path"));

        String method = "getPathParam";
        Class<String> type = String.class;
        callInjectable(method, type);

        assertEquals("getPathParam(String)", lastCall.name());
        assertEquals(List.of("path"), lastCall.arguments());
    }

    @Override
    protected void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        SubResourceLocators.SubResourceLocator locator = new SubResourceLocators.SubResourceLocator(SubResourceMethods.class.getMethod(method, type));
        locator.match(mock(UriTemplate.MatchResult.class), "GET", new String[0], context, builder).get();
    }

    @SuppressWarnings("UnresolvedRestParam")
    interface SubResourceMethods {
        @Path("/message")
        Message getPathParam(@PathParam("param") int value);

        @Path("/message")
        Message getPathParam(@PathParam("param") short value);

        @Path("/message")
        Message getPathParam(@PathParam("param") float value);

        @Path("/message")
        Message getPathParam(@PathParam("param") double value);

        @Path("/message")
        Message getPathParam(@PathParam("param") byte value);

        @Path("/message")
        Message getPathParam(@PathParam("param") boolean value);

        @Path("/message")
        Message getPathParam(@PathParam("param") BigDecimal value);

        @Path("/message")
        Message getPathParam(@PathParam("param") Converter value);

        @Path("/message")
        Message getPathParam(@PathParam("param") String value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") int value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") short value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") float value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") double value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") byte value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") boolean value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") BigDecimal value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") Converter value);

        @Path("/message")
        Message getQueryParam(@QueryParam("param") String value);

        @Path("/message")
        Message getContext(@Context SomeServiceInContext service);

        @Path("/message")
        Message getContext(@Context ResourceContext context);

        @Path("/message")
        Message getContext(@Context UriInfo uriInfo);
    }

    static class Message {
        @GET
        public String content() {
            return "content";
        }
    }
}