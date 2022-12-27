package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    public void setup() {
        resource = mock(CallableResourceMethods.class);
        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        when(builder.getLastMatchedResource()).thenReturn(resource);
    }

    @Test
    void should_call_resource_method() throws Exception {
        when(resource.get()).thenReturn("resource called");
        DefaultResourceMethod resourceMethod = getResourceMethod("get");
        assertEquals(new GenericEntity<>("resource called", String.class), resourceMethod.call(context, builder));
    }

    @Test
    void should_use_resource_method_generic_return_type() throws NoSuchMethodException {
        when(resource.getList()).thenReturn(List.of());
        DefaultResourceMethod resourceMethod = getResourceMethod("getList");
        assertEquals(new GenericEntity<>(List.of(), CallableResourceMethods.class.getMethod("getList").getGenericReturnType()), resourceMethod.call(context, builder));
    }

    //TODO using default converters for path, matrix, query(uri), form, header, cookie(request)
    //TODO default converters for int, short, float, double, byte, char, String, and boolean
    //TODO default converters for class with converter constructor
    //TODO default converters for class with converter factory
    //TODO default converters for List, Set, SortSet
    //TODO injection - get injectable from resource context
    //TODO injection - can inject resource context itself
    //TODO injection - can inject uri info built from uri info builder

    interface CallableResourceMethods {

        @GET
        String get();

        @GET
        List<String> getList();

    }

    private static DefaultResourceMethod getResourceMethod(String methodName) throws NoSuchMethodException {
        return new DefaultResourceMethod(CallableResourceMethods.class.getMethod(methodName));
    }
}
