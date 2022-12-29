package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author qiubaisen
 * @date 2022/12/27
 */
public class DefaultResourceMethodTest extends InjectableCallerTest {

    @Override
    protected Object initResource() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    lastCall = new LastCall(
                            getMethodName(name, method.getParameterTypes()),
                            args == null ? List.of() : List.of(args));
                    return "getList".equals(name) ? List.of() : null;
                });
    }

    @Test
    void should_call_resource_method() throws Exception {
        getResourceMethod("get").call(context, builder);
        assertEquals("get()", lastCall.name());
    }

    @Test
    void should_use_resource_method_generic_return_type() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getList");
        assertEquals(new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()), resourceMethod.call(context, builder));
    }

    @Test
    void should_call_resource_method_with_void_return_type() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("post");

        assertNull(resourceMethod.call(context, builder));
    }


    @Override
    protected void callInjectable(String method, Class<?> type) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        resourceMethod.call(context, builder);
    }


    //TODO using default converters for path, query, matrix(uri), form, header, cookie(request)
    //TODO default converters for List, Set, SortSet, Array

    interface CallableResourceMethods {

        @POST
        void post();

        @GET
        String get();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("param") int value);

        @GET
        String getPathParam(@PathParam("param") short value);

        @GET
        String getPathParam(@PathParam("param") float value);

        @GET
        String getPathParam(@PathParam("param") double value);

        @GET
        String getPathParam(@PathParam("param") byte value);

        @GET
        String getPathParam(@PathParam("param") boolean value);

        @GET
        String getPathParam(@PathParam("param") BigDecimal value);

        @GET
        String getPathParam(@PathParam("param") Converter value);

        @GET
        String getPathParam(@PathParam("param") String value);

        @GET
        String getQueryParam(@QueryParam("param") int value);

        @GET
        String getQueryParam(@QueryParam("param") short value);

        @GET
        String getQueryParam(@QueryParam("param") float value);

        @GET
        String getQueryParam(@QueryParam("param") double value);

        @GET
        String getQueryParam(@QueryParam("param") byte value);

        @GET
        String getQueryParam(@QueryParam("param") boolean value);

        @GET
        String getQueryParam(@QueryParam("param") BigDecimal value);

        @GET
        String getQueryParam(@QueryParam("param") Converter value);

        @GET
        String getQueryParam(@QueryParam("param") String value);

        @GET
        String getContext(@Context SomeServiceInContext service);

        @GET
        String getContext(@Context ResourceContext context);

        @GET
        String getContext(@Context UriInfo uriInfo);
    }

    private static DefaultResourceMethod getResourceMethod(String methodName, Class... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
    }
}

class NoConverter {
    NoConverter valueOf(String value) {
        return new NoConverter();
    }
}

enum Converter {
    Primitive, Constructor, Factory
}

interface SomeServiceInContext {
}