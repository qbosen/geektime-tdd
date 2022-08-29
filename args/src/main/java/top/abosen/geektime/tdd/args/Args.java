package top.abosen.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class Args {

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
        Option option = parameter.getAnnotation(Option.class);
        OptionParser<?> optionParser = getOptionParser(parameter.getType());
        return optionParser.parse(option, arguments);
    }

    private static final Map<Class<?>, OptionParser<?>> PARSER_MAP = Map.of(
            int.class, OptionParsers.createIntParser(),
            String.class, OptionParsers.createStringParser(),
            boolean.class, OptionParsers.createBooleanParser(),
            int[].class, OptionParsers.createIntArrayParser(),
            String[].class, OptionParsers.createStringArrayParser()
    );

    private static <T> OptionParser<T> getOptionParser(Class<T> type) {
        return (OptionParser<T>) PARSER_MAP.get(type);
    }


}
