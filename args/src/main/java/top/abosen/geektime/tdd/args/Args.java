package top.abosen.geektime.tdd.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class Args {
    public static <T> T parse(Class<T> optionClass, String... args) {
        Constructor<?> constructor = optionClass.getDeclaredConstructors()[0];
        Parameter[] parameters = constructor.getParameters();
        List<String> arguments = Arrays.asList(args);
        Parameter parameter = parameters[0];
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
        }else if(parameter.getType() == String[].class){
            int index = arguments.indexOf("-" + option.value());
            value  = arguments.subList(index+1, arguments.size()).toArray(String[]::new);
        } else if (parameter.getType() == int[].class) {
            int index = arguments.indexOf("-" + option.value());
            value  = arguments.subList(index+1, arguments.size()).stream().mapToInt(Integer::parseInt).toArray();
        }

        try {
            return (T) constructor.newInstance(value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
