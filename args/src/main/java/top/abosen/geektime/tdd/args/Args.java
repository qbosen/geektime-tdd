package top.abosen.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class Args {

    public static final String FLAG_PATTERN = "^-[a-zA-Z-]+$";

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
            int.class, new IntParser(),
            String.class, new StringParser(),
            boolean.class, new BooleanParser(),
            int[].class, new IntArrayParser(),
            String[].class, new StringArrayParser()
    );

    private static <T> OptionParser<T> getOptionParser(Class<T> type) {
        return (OptionParser<T>) PARSER_MAP.get(type);
    }

    static class BooleanParser implements OptionParser<Boolean> {
        @Override
        public Boolean parse(Option option, List<String> arguments) {
            Boolean value;
            value = arguments.contains("-" + option.value());
            return value;
        }
    }

    static class StringParser implements OptionParser<String> {
        @Override
        public String parse(Option option, List<String> arguments) {
            String value;
            int index = arguments.indexOf("-" + option.value());
            value = arguments.get(index + 1);
            return value;
        }
    }

    static class StringArrayParser implements OptionParser<String[]> {
        @Override
        public String[] parse(Option option, List<String> arguments) {
            String[] value;
            int index = arguments.indexOf("-" + option.value());
            int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                    .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                    .findFirst().orElseGet(arguments::size);
            value = arguments.subList(index + 1, nextFlagIndex).toArray(String[]::new);
            return value;
        }
    }

    static class IntParser implements OptionParser<Integer> {
        @Override
        public Integer parse(Option option, List<String> arguments) {
            Integer value;
            int index = arguments.indexOf("-" + option.value());
            value = Integer.parseInt(arguments.get(index + 1));
            return value;
        }
    }

    static class IntArrayParser implements OptionParser<int[]> {
        @Override
        public int[] parse(Option option, List<String> arguments) {
            int[] value;
            int index = arguments.indexOf("-" + option.value());
            int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                    .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                    .findFirst().orElseGet(arguments::size);
            value = arguments.subList(index + 1, nextFlagIndex).stream().mapToInt(Integer::parseInt).toArray();
            return value;
        }
    }


    interface OptionParser<T> {
        T parse(Option option, List<String> arguments);
    }
}
