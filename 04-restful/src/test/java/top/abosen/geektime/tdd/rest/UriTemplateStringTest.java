package top.abosen.geektime.tdd.rest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/12/21
 */
public class UriTemplateStringTest {
    @Test
    void should_return_empty_if_path_not_matched() {
        UriTemplateString template = new UriTemplateString("/users");
        Optional<UriTemplate.MatchResult> result = template.match("/orders");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_match_result_if_path_matched() {
        UriTemplateString template = new UriTemplateString("/users");
        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users", result.getMatched());
        assertEquals("/1", result.getRemaining());
    }

    @Test
    void should_return_match_result_if_path_matched_with_variable() {
        UriTemplateString template = new UriTemplateString("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("/users/1", result.getMatched());
        assertNull(result.getRemaining());
        assertFalse(result.getMatchedPathParameters().isEmpty());
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    void should_return_empty_if_not_match_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");
        Optional<UriTemplate.MatchResult> result = template.match("/users/id");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_extract_variable_value_by_given_pattern() {
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    void should_throw_illegal_argument_exception_if_variable_redefined() {
        assertThrows(IllegalArgumentException.class, () -> new UriTemplateString("/users/{id:[0-9]+}/{id}"));
    }

    //TODO comparing result, with match Literal, variables, and specific variables
}
