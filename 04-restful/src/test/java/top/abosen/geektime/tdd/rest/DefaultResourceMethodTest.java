package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
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
    private SomeServiceInContext service;

    record LastCall(String name, List<Object> arguments) {

    }

    @BeforeEach
    public void setup() {
        lastCall = null;
        resource = (CallableResourceMethods) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{CallableResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    lastCall = new LastCall(
                            getMethodName(name, method.getParameterTypes()),
                            args == null ? List.of() : List.of(args));
                    return "getList".equals(name) ? List.of() : null;
                });

        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        uriInfo = mock(UriInfo.class);
        service = mock(SomeServiceInContext.class);
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
        when(context.getResource(eq(SomeServiceInContext.class))).thenReturn(service);
    }

    private static String getMethodName(String name, Class<?>... types) {
        return name + Arrays.stream(types)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
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


    record InjectableTypeTestCase(Class<?> type, String string, Object value) {
    }


    @TestFactory
    public List<DynamicTest> inject_convertable_types() {
        List<DynamicTest> tests = new ArrayList<>();
        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(int.class, "1", 1),
                new InjectableTypeTestCase(short.class, "3", (short) 3),
                new InjectableTypeTestCase(float.class, "8.33", 8.33f),
                new InjectableTypeTestCase(double.class, "3.25", 3.25d),
                new InjectableTypeTestCase(byte.class, "111", (byte) 111),
                new InjectableTypeTestCase(boolean.class, "true", true),
                new InjectableTypeTestCase(BigDecimal.class, "12345", new BigDecimal("12345")),
                new InjectableTypeTestCase(Converter.class, "Factory", Converter.Factory),
                new InjectableTypeTestCase(String.class, "string", "string")
        );
        List<String> paramTypes = List.of("getPathParam", "getQueryParam");

        for (String type : paramTypes) {
            for (InjectableTypeTestCase typeCase : typeCases) {
                tests.add(DynamicTest.dynamicTest(
                        MessageFormat.format("should inject {0} to {1}", typeCase.type.getSimpleName(), type),
                        () -> verifyResourceMethodCalled(type, typeCase.type, typeCase.string, typeCase.value)
                ));
            }
        }
        return tests;
    }

    @TestFactory
    public List<DynamicTest> inject_context_object() {
        List<DynamicTest> tests = new ArrayList<>();
        List<InjectableTypeTestCase> typeCases = List.of(
                new InjectableTypeTestCase(SomeServiceInContext.class, "N/A", service),
                new InjectableTypeTestCase(ResourceContext.class, "N/A", context),
                new InjectableTypeTestCase(UriInfo.class, "N/A", uriInfo)
        );
        List<String> paramTypes = List.of("getContext");

        for (String type : paramTypes) {
            for (InjectableTypeTestCase typeCase : typeCases) {
                tests.add(DynamicTest.dynamicTest(
                        MessageFormat.format("should inject {0} to getContext {1}", typeCase.type.getSimpleName(), type),
                        () -> verifyResourceMethodCalled(type, typeCase.type, typeCase.string, typeCase.value)
                ));
            }
        }
        return tests;
    }

    private void verifyResourceMethodCalled(String method, Class<?> type, String parameterValue, Object value) throws NoSuchMethodException {
        DefaultResourceMethod resourceMethod = getResourceMethod(method, type);
        parameters.put("param", List.of(parameterValue));
        resourceMethod.call(context, builder);
        assertEquals(getMethodName(method, type), lastCall.name);
        assertEquals(List.of(value), lastCall.arguments);
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