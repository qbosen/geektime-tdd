package top.abosen.geektime.tdd.args;

import top.abosen.geektime.tdd.args.exceptions.InsufficientArgumentsException;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class OptionParsers {
    private OptionParsers() {
    }

    private static final String FLAG_PATTERN = "^-[a-zA-Z-]+$";

    public static OptionParser<Boolean> createBooleanParser() {
        return (option, arguments) -> getOptionArguments(option, arguments, 0).isPresent();
    }

    public static OptionParser<Integer[]> createIntegerArrayParser() {
        return createArrayValueParser(Integer::parseInt, Integer[]::new);
    }

    public static OptionParser<int[]> createIntArrayParser() {
        return (option, arguments) -> Stream.of(createIntegerArrayParser().parse(option, arguments))
                .mapToInt(Integer::intValue).toArray();
    }

    public static OptionParser<Integer> createIntParser() {
        return createSingleValueParser(Integer::parseInt);
    }

    public static OptionParser<String[]> createStringArrayParser() {
        return createArrayValueParser(String::valueOf, String[]::new);
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

    private static <T> OptionParser<T[]> createArrayValueParser(Function<String, T> parser, IntFunction<T[]> generator) {
        return (option, arguments) -> getOptionArguments(option, arguments)
                .map(it -> it.stream().map(parser).toArray(generator))
                .orElseGet(() -> generator.apply(0));
    }

    private static Optional<List<String>> getOptionArguments(Option option, List<String> arguments, int expectedSize) {
        return getOptionArguments(option, arguments).map(it -> {
            if (it.size() < expectedSize) {
                throw new InsufficientArgumentsException(option.value());
            }
            if (it.size() > expectedSize) {
                throw new TooManyArgumentsException(option.value());
            }
            return it;
        });
    }

    private static Optional<List<String>> getOptionArguments(Option option, List<String> arguments) {
        int index = arguments.indexOf("-" + option.value());
        if (index == -1) return Optional.empty();
        int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                .findFirst().orElseGet(arguments::size);
        return Optional.of(arguments.subList(index + 1, nextFlagIndex));
    }

}
