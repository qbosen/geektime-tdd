package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/10/8
 */
public interface OptionClass<T> {
    String[] getOptionNames();

    Class<?> getOptionType(String name);

    T create(Object[] args);
}
