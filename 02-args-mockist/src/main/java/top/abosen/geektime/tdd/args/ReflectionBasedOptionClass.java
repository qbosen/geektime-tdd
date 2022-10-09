package top.abosen.geektime.tdd.args;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * @author qiubaisen
 * @date 2022/10/9
 */
public class ReflectionBasedOptionClass<T> implements OptionClass<T> {

    private final Class<T> optionClass;

    public ReflectionBasedOptionClass(Class<T> optionClass) {
        this.optionClass = optionClass;
    }

    @Override
    public String[] getOptionNames() {
        return Arrays.stream(optionClass.getDeclaredConstructors()[0].getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(Option.class))
                .map(parameter -> parameter.getAnnotation(Option.class).value())
                .toArray(String[]::new);
    }

    @Override
    public Class<?> getOptionType(String name) {
        return Arrays.stream(optionClass.getDeclaredConstructors()[0].getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(Option.class))
                .filter(parameter -> name.equals(parameter.getAnnotation(Option.class).value()))
                .findFirst().map(Parameter::getType)
                .orElseThrow(()->new RuntimeException("no option found"));
    }

    @Override
    public T create(Object[] args) {
        try {
            return (T) optionClass.getDeclaredConstructors()[0].newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
