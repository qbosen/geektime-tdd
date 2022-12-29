package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/29
 */
public abstract class InjectableCallerTest {
    protected ResourceContext context;
    protected UriInfoBuilder builder;
    protected UriInfo uriInfo;
    protected MultivaluedHashMap<String, String> parameters;
    protected DefaultResourceMethodTest.LastCall lastCall;
    protected SomeServiceInContext service;
    private Object resource;

    protected static String getMethodName(String name, Class<?>... types) {
        return name + Arrays.stream(types)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @BeforeEach
    public void setup() {
        lastCall = null;
        resource = initResource();

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

    protected abstract Object initResource();

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
                        MessageFormat.format("should inject {0} to {1}", typeCase.type().getSimpleName(), type),
                        () -> verifyResourceMethodCalled(type, typeCase.type(), typeCase.string(), typeCase.value())
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
                        MessageFormat.format("should inject {0} to getContext {1}", typeCase.type().getSimpleName(), type),
                        () -> verifyResourceMethodCalled(type, typeCase.type(), typeCase.string(), typeCase.value())
                ));
            }
        }
        return tests;
    }

    private void verifyResourceMethodCalled(String method, Class<?> type, String parameterValue, Object value) throws NoSuchMethodException {
        parameters.put("param", List.of(parameterValue));
        callInjectable(method, type);
        assertEquals(getMethodName(method, type), lastCall.name());
        assertEquals(List.of(value), lastCall.arguments());
    }

    protected abstract void callInjectable(String method, Class<?> type) throws NoSuchMethodException;

    record LastCall(String name, List<Object> arguments) {
    }

    record InjectableTypeTestCase(Class<?> type, String string, Object value) {
    }
}
