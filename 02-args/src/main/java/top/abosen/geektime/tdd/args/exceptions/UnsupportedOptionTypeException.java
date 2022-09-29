package top.abosen.geektime.tdd.args.exceptions;

import top.abosen.geektime.tdd.args.Option;

/**
 * @author qiubaisen
 * @date 2022/9/28
 */
public class UnsupportedOptionTypeException extends RuntimeException{

    private final Option option;
    private final Class<?> type;

    public UnsupportedOptionTypeException(Option option, Class<?> type) {
        this.option = option;
        this.type = type;
    }

    public Option getOption() {
        return option;
    }

    public Class<?> getType() {
        return type;
    }
}
