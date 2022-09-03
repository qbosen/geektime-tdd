package top.abosen.geektime.tdd.args;

import top.abosen.geektime.tdd.args.exceptions.IllegalValueException;
import top.abosen.geektime.tdd.args.exceptions.InsufficientArgumentsException;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.util.List;
import java.util.Optional;
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

    public static OptionParser<Boolean> bool() {
        return (option, arguments) -> getOptionArguments(option, arguments, 0).isPresent();
    }

    public static <T> OptionParser<T> unary(Function<String, T> parser, T defaultValue) {
        return (option, arguments) -> getOptionArguments(option, arguments, 1)
                .map(it -> it.get(0)).map(value -> parseValue(option, value, parser)).orElse(defaultValue);
    }

    public static <T> OptionParser<T[]> array(Function<String, T> parser, IntFunction<T[]> generator) {
        return (option, arguments) -> getOptionArguments(option, arguments)
                .map(it -> it.stream().map(value -> parseValue(option, value, parser)).toArray(generator))
                .orElseGet(() -> generator.apply(0));
    }

    /**
     * 基础类型数组转换器
     *
     * @param parser    包装类型映射
     * @param generator 包装类型数组构造器
     * @param <T>       包装类型
     * @param <PA>      基础类型数组
     * @return 基础类型数组转换器
     */
    @SuppressWarnings({"unchecked", "unused"})
    public static <T, PA> OptionParser<PA> primaryArray(Function<String, T> parser, IntFunction<T[]> generator, Class<PA> primaryArrayType) {
        return (option, arguments) -> (PA) ArrayUtils.toPrimitive(array(parser, generator).parse(option, arguments));
    }

    private static <T> T parseValue(Option option, String value, Function<String, T> valueParser) {
        try {
            return valueParser.apply(value);
        } catch (Exception e) {
            throw new IllegalValueException(option.value(), value);
        }
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
