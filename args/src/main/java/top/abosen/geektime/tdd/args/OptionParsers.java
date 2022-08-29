package top.abosen.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class OptionParsers {
    private OptionParsers() {
    }

    private static final String FLAG_PATTERN = "^-[a-zA-Z-]+$";

    public static OptionParser<Integer[]> createIntArrayParser() {
        return new ArrayValueParser<>(Integer::parseInt, Integer[]::new);
    }

    public static OptionParser<Integer> createIntParser() {
        return createSingleValueParser(Integer::parseInt);
    }

    public static OptionParser<String[]> createStringArrayParser() {
        return new ArrayValueParser<>(String::valueOf, String[]::new);
    }

    public static OptionParser<String> createStringParser() {
        return createSingleValueParser(String::valueOf);
    }

    public static <T> OptionParser<T> createSingleValueParser(Function<String, T> parser) {
        return (option, arguments) -> {
            int index = arguments.indexOf("-" + option.value());
            return parser.apply(arguments.get(index + 1));
        };
    }

    static class ArrayValueParser<T> implements OptionParser<T[]> {
        private final Function<String, T> parser;
        private final IntFunction<T[]> generator;

        public ArrayValueParser(Function<String, T> parser, IntFunction<T[]> generator) {
            this.parser = parser;
            this.generator = generator;
        }

        @Override
        public T[] parse(Option option, List<String> arguments) {
            int index = arguments.indexOf("-" + option.value());
            int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                    .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                    .findFirst().orElseGet(arguments::size);
            return arguments.subList(index + 1, nextFlagIndex).stream().map(parser).toArray(generator);
        }
    }

    static class BooleanParser implements OptionParser<Boolean> {
        private BooleanParser() {
        }

        public static OptionParser<Boolean> createBooleanParser() {
            return new BooleanParser();
        }

        @Override
        public Boolean parse(Option option, List<String> arguments) {
            Boolean value;
            value = arguments.contains("-" + option.value());
            return value;
        }
    }
}
