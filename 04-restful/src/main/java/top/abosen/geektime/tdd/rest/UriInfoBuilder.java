package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

/**
 * @author qiubaisen
 * @date 2022/12/15
 */
public interface UriInfoBuilder {
//    void pushMatchedPath(String path);
//
//    void addParameter(String name, String value);
//
//    String getUnmatchedPath();

    Object getLastMatchedResource();

    void addMatchedResource(Object resource);
    void addMatchedPathParameter(Map<String,String> pathParameter);

    UriInfo createUriInfo();
}
