package top.abosen.geektime.tdd.rest;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author qiubaisen
 * @date 2022/12/23
 */
interface UriHandler {
    UriTemplate getUriTemplate();
}

class UriHandlers {

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers,
                                                           Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return matched(path, handlers, matchFunction).map(Result::handler);
    }

    public static <T extends UriHandler, R> Optional<R> match(
            String path, List<T> handlers,
            BiFunction<Optional<UriTemplate.MatchResult>, T, Optional<R>> mapper
    ) {
        return matched(path, handlers, r -> true).flatMap(r -> mapper.apply(r.matched, r.handler));
    }

    public static <T extends UriHandler> Optional<T> match(String path, List<T> handlers) {
        return match(path, handlers, r -> true);
    }

    private static <T extends UriHandler> Optional<Result<T>> matched(String path, List<T> handlers, Function<UriTemplate.MatchResult, Boolean> matchFunction) {
        return handlers.stream()
                .map(handler -> new Result<>(handler.getUriTemplate().match(path), handler, matchFunction))
                .filter(Result::isMatched)
                .sorted()
                .findFirst();
    }

    private record Result<T extends UriHandler>(
            Optional<UriTemplate.MatchResult> matched,
            T handler, Function<UriTemplate.MatchResult, Boolean> matchFunction)
            implements Comparable<Result<T>> {

        public boolean isMatched() {
            return matched.map(matchFunction).orElse(false);
        }

        @Override
        public int compareTo(Result<T> o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }
}