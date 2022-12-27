package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/27
 */
public class DefaultResourceMethodTest {

    @Test
    void should_call_resource_method() throws Exception{
        CallableResourceMethods resource = mock(CallableResourceMethods.class);
        ResourceContext context = mock(ResourceContext.class);
        UriInfoBuilder builder = mock(UriInfoBuilder.class);
        DefaultResourceMethod resourceMethod = new DefaultResourceMethod(CallableResourceMethods.class.getMethod("get"));
        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(resource.get()).thenReturn("resource called");

        GenericEntity<?> entity = resourceMethod.call(context, builder);
        Assertions.assertEquals(new GenericEntity<>("resource called", String.class), entity);
    }

    //TODO return type, List<String>
    //TODO injection - context
    //TODO injection - uri info: path, query, matrix...

    interface CallableResourceMethods {
        @GET
        String get();
    }
}
