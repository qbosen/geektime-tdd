package top.abosen.geektime.tdd.rest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final int variableGroupStartFrom;

    private static String group(String pattern) {
        return group(pattern, false);
    }

    private static String group(String pattern, boolean optional) {
        return "(" + pattern + ")" + (optional ? "?" : "");
    }

    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(variable(template)) + "(/.*)?");
        variableGroupStartFrom = 2;
    }

    private String variable(String template) {
        return variable.matcher(template).replaceAll(result -> {
            String variableName = result.group(variableNameGroup);
            String pattern = result.group(variablePatternGroup);
            if (variables.contains(variableName)) {
                throw new IllegalArgumentException("duplicate variable: " + variableName);
            }
            variables.add(variableName);
            return pattern == null ? DefaultVariablePattern : group(pattern);
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new PathMatchResult(matcher));
    }


    class PathMatchResult implements MatchResult {

        private final Matcher matcher;
        private final int count;
        private final Map<String, String> parameters;
        private int matchLiteralCount;

        public PathMatchResult(Matcher matcher) {
            this.matcher = matcher;
            this.count = matcher.groupCount();
            this.matchLiteralCount = matcher.group(1).length();
            this.parameters = new HashMap<>();
            for (int i = 0; i < variables.size(); i++) {
                if (parameters.containsKey(variables.get(i))) throw new IllegalStateException("Duplicate key");
                this.matchLiteralCount -= matcher.group(i + variableGroupStartFrom).length();
                parameters.put(variables.get(i), matcher.group(i + variableGroupStartFrom));
            }
        }

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
            PathMatchResult other = (PathMatchResult) o;
            if (this.matchLiteralCount > other.matchLiteralCount) return -1;
            if (this.parameters.size() > other.parameters.size()) return -1;
            return 0;
        }
    }
}
