package top.abosen.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * @author qiubaisen
 * @date 2022/9/29
 */
public class Args<T> {

    private final Class<T> optionClass;
    private final Map<Class<?>, OptionParser<?>> parsers;

    protected Args(Class<T> optionClass, Map<Class<?>, OptionParser<?>> parsers) {
        this.parsers = parsers;
        this.optionClass = optionClass;
    }


    public T parse(String... args) {
        Map<String, String[]> options = toMap(args);
        Constructor<?> constructor = optionClass.getDeclaredConstructors()[0];
        Object[] values = Arrays.stream(constructor.getParameters()).map(it -> parseOption(options, it)).toArray();
        try {
            return (T) constructor.newInstance(values);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseOption(Map<String, String[]> options, Parameter parameter) {
        if (!parameter.isAnnotationPresent(Option.class)) {
            throw new RuntimeException();
        }
        if (!parsers.containsKey(parameter.getType())) {
            throw new RuntimeException();
        }
        Option option = parameter.getAnnotation(Option.class);
        return parsers.get(parameter.getType()).parse(options.get(option.value()));
    }

    public static Map<String, String[]> toMap(String... args) {
        Map<String, String[]> result = new HashMap<>();

        String option = null;
        List<String> values = new ArrayList<>();
        for (String arg : args) {
            if (arg.matches("^-[a-zA-Z-]+$")) {
                if (option != null) {
                    result.put(option.substring(1), values.toArray(String[]::new));
                }
                option = arg;
                values = new ArrayList<>();
            } else {
                values.add(arg);
            }
        }
        if (option != null) {
            result.put(option.substring(1), values.toArray(String[]::new));
        }

        return result;
    }
}
