package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/12/23
 */
class StubUriInfoBuilder implements UriInfoBuilder {
    private List<Object> matchedResource = new ArrayList<>();

    public StubUriInfoBuilder() {
    }

    @Override
    public Object getLastMatchedResource() {
        return matchedResource.get(matchedResource.size() - 1);
    }

    @Override
    public void addMatchedResource(Object resource) {
        matchedResource.add(resource);
    }

    @Override
    public UriInfo createUriInfo() {
        return null;
    }
}
