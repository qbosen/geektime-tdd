package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/9/29
 */
@FunctionalInterface
public interface OptionParser<T> {
    T parse(String[] args);
}
