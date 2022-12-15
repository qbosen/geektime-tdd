package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.Path;

import java.util.Map;
import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/12/15
 */
interface UriTemplate {
    @Path(("/{id}")) // /1/orders
    interface MatchResult extends Comparable<MatchResult> {
        String getMatchedPath();    // /1

        String getRemaining(); // orders

        Map<String, String> getMatchedPathParameters();     //{id:1}
    }

    Optional<MatchResult> match(String path);
}
