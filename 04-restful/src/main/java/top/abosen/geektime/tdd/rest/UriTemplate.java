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

class PathTemplate implements UriTemplate {

    private final Pattern pattern;
    private final PathVariables pathVariables = new PathVariables();
    private final int variableGroupStartFrom;

    public PathTemplate(String template) {
        pattern = Pattern.compile(group(pathVariables.template(template)) + "(/.*)?");
        variableGroupStartFrom = 2;
    }

    private static String group(String pattern, boolean optional) {
        return "(" + pattern + ")" + (optional ? "?" : "");
    }

    private static String group(String pattern) {
        return group(pattern, false);
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) return Optional.empty();
        return Optional.of(new PathMatchResult(matcher, pathVariables));
    }

    class PathVariables implements Comparable<PathVariables> {

        public static final String DefaultVariablePattern = "([^/]+?)";
        private static final String LeftBracket = "\\{";
        private static final String RightBracket = "}";
        private static final String VariableName = "\\w[\\w.-]*";
        private static final String NonBrackets = "[^\\{}]+";
        private static final Pattern variable = Pattern.compile(LeftBracket + group(VariableName) + group(":" + group(NonBrackets), true) + RightBracket);
        private static final int variableNameGroup = 1;
        private static final int variablePatternGroup = 3;
        private final List<String> variables = new ArrayList<>();
        private int specificParameterCount = 0;

        private String template(String template) {
            return variable.matcher(template).replaceAll(pathVariables::replace);
        }

        private String replace(java.util.regex.MatchResult result) {
            String variableName = result.group(variableNameGroup);
            String pattern = result.group(variablePatternGroup);
            if (variables.contains(variableName)) {
                throw new IllegalArgumentException("duplicate variable: " + variableName);
            }
            variables.add(variableName);
            if (pattern != null) {
                specificParameterCount++;
                return group(pattern);
            }
            return DefaultVariablePattern;
        }


        private Map<String, String> extract(Matcher matcher) {
            Map<String, String> parameters = new HashMap<>();
            for (int i = 0; i < variables.size(); i++) {
                parameters.put(variables.get(i), matcher.group(i + variableGroupStartFrom));
            }
            return parameters;
        }

        @Override
        public int compareTo(PathVariables o) {
            return Comparator.<PathVariables, Integer>comparing(it -> it.variables.size(), Comparator.reverseOrder())
                    .thenComparing(it -> it.specificParameterCount, Comparator.reverseOrder())
                    .compare(this, o);
        }
    }

    class PathMatchResult implements MatchResult {

        private final Matcher matcher;
        private final Map<String, String> parameters;
        private final PathVariables variables;
        private final int matchLiteralCount;

        public PathMatchResult(Matcher matcher, PathVariables variable) {
            this.matcher = matcher;
            this.variables = variable;
            this.parameters = variable.extract(matcher);
            this.matchLiteralCount = matcher.group(1).length() - parameters.values().stream().mapToInt(String::length).sum();
        }

        @Override
        public String getMatched() {
            return matcher.group(1);
        }

        @Override
        public String getRemaining() {
            return matcher.group(matcher.groupCount());
        }

        @Override
        public Map<String, String> getMatchedPathParameters() {
            return parameters;
        }

        @Override
        public int compareTo(MatchResult o) {
            return Comparator.<PathMatchResult, Integer>comparing(it -> it.matchLiteralCount, Comparator.reverseOrder())
                    .thenComparing(it -> it.variables)
                    .compare(this, (PathMatchResult) o);
        }
    }
}
