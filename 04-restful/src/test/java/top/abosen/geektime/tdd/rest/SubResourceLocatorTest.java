package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
class SubResourceLocatorTest {
    private SubResourceMethods resource;
    private ResourceContext context;
    private UriInfoBuilder builder;
    private UriInfo uriInfo;
    private MultivaluedHashMap<String, String> parameters;

    private LastCall lastCall;
    private UriTemplate.MatchResult matchResult;

    record LastCall(String name, List<Object> arguments) {

    }

    @BeforeEach
    public void before() {
        lastCall = null;
        resource = (SubResourceMethods) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{SubResourceMethods.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    lastCall = new LastCall(
                            getMethodName(name, method.getParameterTypes()),
                            args == null ? List.of() : List.of(args));
                    return new Message();
                });

        context = mock(ResourceContext.class);
        builder = mock(UriInfoBuilder.class);
        uriInfo = mock(UriInfo.class);
        matchResult = mock(UriTemplate.MatchResult.class);
        parameters = new MultivaluedHashMap<>();

        when(builder.getLastMatchedResource()).thenReturn(resource);
        when(builder.createUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(parameters);
        when(uriInfo.getQueryParameters()).thenReturn(parameters);
    }
    private static String getMethodName(String name, Class<?>... types) {
        return name + Arrays.stream(types)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "(", ")"));
    }

    @Test
    void should_inject_string_path_param_to_sub_resource_method() throws NoSuchMethodException {
        Method method = SubResourceMethods.class.getMethod("getPathParam", String.class);
        SubResourceLocators.SubResourceLocator locator = new SubResourceLocators.SubResourceLocator(method);
        parameters.put("param", List.of("path"));
        locator.match(matchResult, "GET", new String[0], context, builder).get();

        assertEquals("getPathParam(String)", lastCall.name);
        assertEquals(List.of("path"), lastCall.arguments);
    }

    @SuppressWarnings("UnresolvedRestParam")
    interface SubResourceMethods {
        @Path("/message")
        Message getPathParam(@PathParam("param") String path);
    }

    static class Message {
        @GET
        public String content() {
            return "content";
        }
    }
}