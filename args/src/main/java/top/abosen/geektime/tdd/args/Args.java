package top.abosen.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
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
        Object value = null;
        Option option = parameter.getAnnotation(Option.class);

        if (parameter.getType() == boolean.class) {
            value = arguments.contains("-" + option.value());
        } else if (parameter.getType() == int.class) {
            int index = arguments.indexOf("-" + option.value());
            value = Integer.parseInt(arguments.get(index + 1));
        } else if (parameter.getType() == String.class) {
            int index = arguments.indexOf("-" + option.value());
            value = arguments.get(index + 1);
        } else if (parameter.getType() == String[].class) {
            int index = arguments.indexOf("-" + option.value());
            int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                    .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                    .findFirst().orElseGet(arguments::size);
            value = arguments.subList(index + 1, nextFlagIndex).toArray(String[]::new);
        } else if (parameter.getType() == int[].class) {
            int index = arguments.indexOf("-" + option.value());
            int nextFlagIndex = IntStream.range(index + 1, arguments.size())
                    .filter(it -> arguments.get(it).matches(FLAG_PATTERN))
                    .findFirst().orElseGet(arguments::size);
            value = arguments.subList(index + 1, nextFlagIndex).stream().mapToInt(Integer::parseInt).toArray();
        }
        return value;
    }

}
