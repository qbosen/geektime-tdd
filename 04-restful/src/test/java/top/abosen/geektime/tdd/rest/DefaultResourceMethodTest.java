package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/27
 */
public class DefaultResourceMethodTest {

    private CallableResourceMethods resource;
    private ResourceContext context;
    private UriInfoBuilder builder;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    private LastCall lastCall;

    record LastCall(String name, List<Object> arguments) {

    }

    @BeforeEach
    public void setup() {
        lastCall = null;
        resource = (CallableResourceMethods) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    lastCall = new LastCall(method.getName() + Arrays.stream(method.getParameters())
                            .map(it -> it.getType().getSimpleName())
                            .collect(Collectors.joining(",", "(", ")")),
                            args == null ? List.of() : List.of(args));
                    return "getList".equals(method.getName()) ? List.of() : null;
                });

        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        uriInfo = mock(UriInfo.class);
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
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
    void should_inject_string_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", String.class);
        parameters.put("path", List.of("path"));
        resourceMethod.call(context, builder);
        assertEquals("getPathParam(String)", lastCall.name);
        assertEquals(List.of("path"), lastCall.arguments);
    }

    @Test
    void should_call_resource_method_with_void_return_type() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("post");

        assertNull(resourceMethod.call(context, builder));
    }

    @Test
    void should_inject_int_to_path_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getPathParam", int.class);
        parameters.put("path", List.of("1"));
        resourceMethod.call(context, builder);

        assertEquals("getPathParam(int)", lastCall.name);
        assertEquals(List.of(1), lastCall.arguments);
    }

    @Test
    void should_inject_string_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", String.class);
        parameters.put("query", List.of("query"));

        resourceMethod.call(context, builder);

        assertEquals("getQueryParam(String)", lastCall.name);
        assertEquals(List.of("query"), lastCall.arguments);
    }

    @Test
    void should_inject_int_to_query_param() throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod("getQueryParam", int.class);
        parameters.put("query", List.of("1"));

        resourceMethod.call(context, builder);
        assertEquals("getQueryParam(int)", lastCall.name);
        assertEquals(List.of(1), lastCall.arguments);
    }

    //TODO using default converters for path, query, matrix(uri), form, header, cookie(request)
    //TODO default converters for int, short, float, double, byte, char, String, and boolean
    //TODO default converters for class with converter constructor
    //TODO default converters for class with converter factory
    //TODO default converters for List, Set, SortSet
    //TODO injection - get injectable from resource context
    //TODO injection - can inject resource context itself
    //TODO injection - can inject uri info built from uri info builder

    interface CallableResourceMethods {

        @POST
        void post();

        @GET
        String get();

        @GET
        List<String> getList();

        @GET
        String getPathParam(@PathParam("path") String value);

        @GET
        String getPathParam(@PathParam("path") int value);

        @GET
        String getQueryParam(@QueryParam("query") String value);

        @GET
        String getQueryParam(@QueryParam("query") int value);
    }

    private static DefaultResourceMethod getResourceMethod(String methodName, Class... types) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName, types));
    }
}
