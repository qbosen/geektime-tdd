package top.abosen.geektime.tdd.args;

import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
interface OptionParser<T> {
    T parse(Option option, List<String> arguments);
}
