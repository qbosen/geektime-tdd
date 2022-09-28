package top.abosen.geektime.tdd.args;

import top.abosen.geektime.tdd.args.exceptions.IllegalOptionException;
import top.abosen.geektime.tdd.args.exceptions.UnsupportedOptionTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Args<T> {
    private static final Map<Class<?>, OptionParser<?>> PARSERS = Map.of(
            boolean.class, OptionParsers.bool(),
            int.class, OptionParsers.unary(0, Integer::parseInt),
            String.class, OptionParsers.unary("", String::valueOf),
            String[].class, OptionParsers.list(String[]::new, String::valueOf),
            Integer[].class, OptionParsers.list(Integer[]::new, Integer::parseInt)
    );

    public static <T> T parse(Class<T> optionsClass, String... args) {
        return new Args<T>(optionsClass, PARSERS).parse(args);
    }

    /**
     * 单元化改造
     * 实现不再依赖于 {@code PARSERS} 这样的外部配置
     * 保持对外的接口不变,通过实例化使得外部依赖可替换(保变原则)
     */
    private final Class<T> optionsClass;
    private final Map<Class<?>, OptionParser<?>> parsers;


    protected Args(Class<T> optionsClass, Map<Class<?>, OptionParser<?>> parsers) {
        this.optionsClass = optionsClass;
        this.parsers = parsers;
    }

    private T parse(String... args) {
        try {
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            List<String> arguments = Arrays.asList(args);
            Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(it, arguments)).toArray();
            //noinspection unchecked
            return (T) constructor.newInstance(values);
        } catch (IllegalOptionException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseOption(Parameter parameter, List<String> arguments) {
        if (!parameter.isAnnotationPresent(Option.class)) throw new IllegalOptionException(parameter.getName());
        Option option = parameter.getAnnotation(Option.class);
        Class<?> parameterType = parameter.getType();
        if (!parsers.containsKey(parameterType)) {
            throw new UnsupportedOptionTypeException(option, parameterType);
        }
        return parsers.get(parameterType).parse(arguments, option);
    }

}
