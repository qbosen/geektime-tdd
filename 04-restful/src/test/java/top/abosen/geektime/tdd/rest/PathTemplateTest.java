package top.abosen.geektime.tdd.rest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/12/21
 */
public class PathTemplateTest {
    @Test
    void should_return_empty_if_path_not_matched() {
        PathTemplate template = new PathTemplate("/users");
        Optional<UriTemplate.MatchResult> result = template.match("/orders");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_return_match_result_if_path_matched() {
        PathTemplate template = new PathTemplate("/users");
        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users", result.getMatched());
        assertEquals("/1", result.getRemaining());
    }

    @Test
    void should_return_match_result_if_path_matched_with_variable() {
        PathTemplate template = new PathTemplate("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("/users/1", result.getMatched());
        assertNull(result.getRemaining());
        assertFalse(result.getMatchedPathParameters().isEmpty());
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    void should_return_empty_if_not_match_given_pattern() {
        PathTemplate template = new PathTemplate("/users/{id:[0-9]+}");
        Optional<UriTemplate.MatchResult> result = template.match("/users/id");
        assertTrue(result.isEmpty());
    }

    @Test
    void should_extract_variable_value_by_given_pattern() {
        PathTemplate template = new PathTemplate("/users/{id:[0-9]+}");
        UriTemplate.MatchResult result = template.match("/users/1").get();
        assertEquals("1", result.getMatchedPathParameters().get("id"));
    }

    @Test
    void should_throw_illegal_argument_exception_if_variable_redefined() {
        assertThrows(IllegalArgumentException.class, () -> new PathTemplate("/users/{id:[0-9]+}/{id}"));
    }

    @Test
    void should_compare_for_match_literal() {
        assertSmaller("/users/1234", "/users/1234", "/users/{id}");
    }

    @Test
    void should_compare__match_variables_if_matched_literal_same() {
        assertSmaller("/users/1234567890/order", "/{resources}/1234567890/{action}", "/users/{id}/order");
    }

    @Test
    void should_compare_specific_variable_if_matched_literal_variable_same() {
        assertSmaller("/users/1", "/users/{id:[0-9]+}", "/users/{id}");
    }

    @Test
    void should_compare_equal_match_result() {
        PathTemplate template = new PathTemplate("/users/{id}");
        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals(0, result.compareTo(result));
    }


    private static void assertSmaller(String path, String smallerTemplate, String largerTemplate) {
        PathTemplate smaller = new PathTemplate(smallerTemplate);
        PathTemplate larger = new PathTemplate(largerTemplate);

        UriTemplate.MatchResult lhs = smaller.match(path).get();
        UriTemplate.MatchResult rhs = larger.match(path).get();

        assertTrue(lhs.compareTo(rhs) < 0);
        assertTrue(rhs.compareTo(lhs) > 0);
    }
}
