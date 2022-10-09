package top.abosen.geektime.tdd.args;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;

class OptionParsers implements OptionParser {

    private static Map<Class<?>, TypedOptionParser<?>> PARSERS = Map.of(
            boolean.class, OptionParsers.bool(),
            int.class, OptionParsers.unary(0, Integer::parseInt),
            String.class, OptionParsers.unary("", String::valueOf),
            String[].class, OptionParsers.list(String[]::new, String::valueOf),
            Integer[].class, OptionParsers.list(Integer[]::new, Integer::parseInt)
    );

    @Override
    public <T> T parse(Class<T> type, String[] optVals) {
        return (T) Optional.ofNullable(PARSERS.get(type))
                .map(func -> func.parse(optVals))
                .orElseThrow(() -> new RuntimeException("type not support"));
    }

    interface TypedOptionParser<T> {
        T parse(String[] args);
    }

    public static TypedOptionParser<Boolean> bool() {
        return (arguments) -> values(arguments, 0).map(it -> true).orElse(false);
    }

    public static <T> TypedOptionParser<T> unary(T defaultValue, Function<String, T> valueParser) {
        return (arguments) -> values(arguments, 1).map(it -> parseValue(it[0], valueParser)).orElse(defaultValue);
    }

    public static <T> TypedOptionParser<T[]> list(IntFunction<T[]> generator, Function<String, T> valueParser) {
        return (arguments) -> Optional.ofNullable(arguments)
                .map(list -> Arrays.stream(list).map(value -> parseValue(value, valueParser)).toArray(generator))
                .orElseGet(() -> generator.apply(0));
    }

    private static <T> T parseValue(String value, Function<String, T> valueParser) {
        try {
            return valueParser.apply(value);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static Optional<String[]> values(String[] arguments, int expectedSize) {
        return Optional.ofNullable(arguments).map(it -> checkSize(expectedSize, it));
    }

    private static String[] checkSize(int expectedSize, String[] values) {
        if (values.length < expectedSize) {
            throw new RuntimeException();
        }
        if (values.length > expectedSize) {
            throw new RuntimeException();
        }
        return values;
    }
}
