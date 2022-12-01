package top.abosen.geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;
import top.abosen.geektime.tdd.di.Context;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
public interface Runtime {
    Providers getProviders();

    ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse response);

    Context getApplicationContext();

    ResourceRouter getResourceRouter();
}
