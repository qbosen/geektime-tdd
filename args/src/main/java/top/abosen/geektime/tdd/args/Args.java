package top.abosen.geektime.tdd.args;

import top.abosen.geektime.tdd.args.exceptions.IllegalOptionException;
import top.abosen.geektime.tdd.args.exceptions.UnsupportedOptionTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
@SuppressWarnings("unchecked")
public class Args {
    private Args() {
    }

    public static <T> T parse(Class<T> optionClass, String... args) {
        Constructor<?> constructor = optionClass.getDeclaredConstructors()[0];
        Parameter[] parameters = constructor.getParameters();
        List<String> arguments = Arrays.asList(args);
        Object[] value = Arrays.stream(parameters).map(p -> parseOption(p, arguments)).toArray();

        try {
            return (T) constructor.newInstance(value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseOption(Parameter parameter, List<String> arguments) {
        if (!parameter.isAnnotationPresent(Option.class)) {
            throw new IllegalOptionException(parameter.getName());
        }
        OptionParser<?> optionParser = getOptionParser(parameter.getType());
        if (Objects.isNull(optionParser)) {
            throw new UnsupportedOptionTypeException(parameter.getType().getSimpleName());
        }
        return optionParser.parse(parameter.getAnnotation(Option.class), arguments);
    }

    private static final Map<Class<?>, OptionParser<?>> PARSER_MAP = Map.of(
            int.class, OptionParsers.unary(Integer::parseInt, 0),
            String.class, OptionParsers.unary(String::valueOf, ""),
            boolean.class, OptionParsers.bool(),
            int[].class, OptionParsers.primaryArray(Integer::parseInt, Integer[]::new, int[].class),
            Integer[].class, OptionParsers.array(Integer::parseInt, Integer[]::new),
            String[].class, OptionParsers.array(String::valueOf, String[]::new)
    );

    private static <T> OptionParser<T> getOptionParser(Class<T> type) {
        return (OptionParser<T>) PARSER_MAP.get(type);
    }


}
