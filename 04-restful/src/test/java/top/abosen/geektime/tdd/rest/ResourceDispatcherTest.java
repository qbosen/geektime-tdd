package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author qiubaisen
 * @date 2022/12/15
 */
public class ResourceDispatcherTest {
    private RuntimeDelegate delegate;
    private Runtime runtime;
    private HttpServletRequest request;
    private ResourceContext context;
    private UriInfoBuilder builder;

    @BeforeEach
    void before() {
        runtime = mock(Runtime.class);

        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createResponseBuilder()).thenReturn(new StubResponseBuilder());

        request = Mockito.mock(HttpServletRequest.class);
        context = Mockito.mock(ResourceContext.class);
        when(request.getServletPath()).thenReturn("/users/1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaders(eq(HttpHeaders.ACCEPT))).thenReturn(new Vector<>(List.of(MediaType.WILDCARD)).elements());

        builder = mock(UriInfoBuilder.class);
        when(runtime.createUriInfoBuilder(same(request))).thenReturn(builder);
    }


    @Test
    void should_use_matched_root_resource() {
        GenericEntity entity = new GenericEntity("matched", String.class);

        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1")), returns(entity)),
                rootResource(unmatched("/users"))));

        OutboundResponse response = router.dispatch(request, context);
        assertSame(entity, response.getGenericEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void should_sort_matched_root_resource_descending_order() {
        GenericEntity entity1 = new GenericEntity("1", String.class);
        GenericEntity entity2 = new GenericEntity("2", String.class);
        DefaultResourceRouter router = new DefaultResourceRouter(runtime, List.of(
                rootResource(matched("/users/1", result("/1", 2)), returns(entity2)),
                rootResource(matched("/users/1", result("/1", 1)), returns(entity1))
        ));

        OutboundResponse response = router.dispatch(request, context);
        assertSame(entity1, response.getGenericEntity());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private static ResourceRouter.RootResource rootResource(UriTemplate unmatchedUriTemplate) {
        ResourceRouter.RootResource unmatched = mock(ResourceRouter.RootResource.class);
        when(unmatched.getUriTemplate()).thenReturn(unmatchedUriTemplate);
        return unmatched;
    }

    private static UriTemplate unmatched(String path) {
        UriTemplate unmatchedUriTemplate = mock(UriTemplate.class);
        when(unmatchedUriTemplate.match(eq(path))).thenReturn(Optional.empty());
        return unmatchedUriTemplate;
    }

    private ResourceRouter.RootResource rootResource(UriTemplate matchedUriTemplate, ResourceRouter.ResourceMethod method) {
        ResourceRouter.RootResource matched = mock(ResourceRouter.RootResource.class);
        when(matched.getUriTemplate()).thenReturn(matchedUriTemplate);
        when(matched.match(eq("/1"), eq("GET"), eq(new String[]{MediaType.WILDCARD}), eq(builder))).thenReturn(Optional.of(method));
        return matched;
    }

    private ResourceRouter.ResourceMethod returns(GenericEntity entity) {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);
        when(method.call(same(context), same(builder))).thenReturn(entity);
        return method;
    }

    private static UriTemplate matched(String path, UriTemplate.MatchResult result) {
        UriTemplate matchedUriTemplate = mock(UriTemplate.class);
        when(matchedUriTemplate.match(eq(path))).thenReturn(Optional.of(result));
        return matchedUriTemplate;
    }

    private static UriTemplate.MatchResult result(String path) {
        return new FakeMathResult(path, 0);
    }

    private static UriTemplate.MatchResult result(String path, int order) {
        return new FakeMathResult(path, order);
    }

    static class FakeMathResult implements UriTemplate.MatchResult {
        private String remaining;
        private int order;

        public FakeMathResult(String remaining, int order) {
            this.remaining = remaining;
            this.order = order;
        }

        @Override
        public String getMatchedPath() {
            return null;
        }

        @Override
        public String getRemaining() {
            return remaining;
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return null;
        }

        @Override
        public int compareTo(UriTemplate.MatchResult o) {
            return Integer.compare(this.order, ((FakeMathResult) o).order);
        }
    }

    //TODO 如果没有匹配的RootResource，则构造404的Response
    //TODO 如果返回的RootResource中无法匹配剩余Path，则构造404的Response
    //TODO 如果ResourceMethod返回nulL，则构造204的Response


}
