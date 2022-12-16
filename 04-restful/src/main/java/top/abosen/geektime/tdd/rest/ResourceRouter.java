package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> match(String path, String method, String[] mediaType, UriInfoBuilder builder);
    }

    interface RootResource extends Resource {
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod {
        GenericEntity<?> call(ResourceContext resourceContext, UriInfoBuilder builder);
    }
}

class DefaultResourceRouter implements ResourceRouter {

    private Runtime runtime;
    private List<RootResource> rootResources;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.runtime = runtime;
        this.rootResources = rootResources;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();

        UriInfoBuilder uri = runtime.createUriInfoBuilder(request);
        Optional<Result> matched = rootResources.stream().map(it -> new Result(it.getUriTemplate().match(path), it))
                .filter(it -> it.matched.isPresent())
                .sorted(Comparator.comparing(it -> it.matched.get()))
                .findFirst();

        Optional<ResourceMethod> method = matched.flatMap(resource -> resource.resource.match(resource.matched.get().getRemaining(), request.getMethod(),
                Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new),
                uri
        ));

        GenericEntity<?> entity = method.map(m -> m.call(resourceContext, uri)).get();

        return (OutboundResponse) OutboundResponse.ok(entity).build();
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) {
    }

}