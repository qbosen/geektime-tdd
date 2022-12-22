package top.abosen.geektime.tdd.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author qiubaisen
 * @date 2022/12/15
 */
interface UriTemplate {
    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getMatchedPathParameters();
    }

    Optional<MatchResult> match(String path);


}

class UriTemplateString implements UriTemplate {

    private static final String LeftBracket = "\\{";
    private static final String RightBracket = "}";
    private static final String VariableName = "\\w[\\w.-]*";
    private static final String NonBrackets = "[^\\{}]+";
    public static final String DefaultVariablePattern = "([^/]+?)";
    private final Pattern pattern;
    private static final Pattern variable = Pattern.compile(LeftBracket + group(VariableName) + group(":" + group(NonBrackets), true) + RightBracket);
    private static final int variableNameGroup = 1;
    private static final int variablePatternGroup = 3;
    private final List<String> variables = new ArrayList<>();
    private final int variableStartFrom;

    private static String group(String pattern) {
        return group(pattern, false);
    }

    private static String group(String pattern, boolean optional) {
        return "(" + pattern + ")" + (optional ? "?" : "");
    }

    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(variable(template)) + "(/.*)?");
        variableStartFrom = 2;
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll(result -> {
            variables.add(result.group(variableNameGroup));
            return result.group(variablePatternGroup) == null ? DefaultVariablePattern : group(result.group(variablePatternGroup));
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        int count = matcher.groupCount();

        Map<String, String> parameters = IntStream.range(0, variables.size()).boxed()
                .collect(Collectors.toMap(variables::get, i -> matcher.group(i  + variableStartFrom)));

        return Optional.of(new MatchResult() {
            @Override
            public String getMatched() {
                return matcher.group(1);
            }

            @Override
            public String getRemaining() {
                return matcher.group(count);
            }

            @Override
            public Map<String, String> getMatchedPathParameters() {
                return parameters;
            }

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }
        });
    }
}
