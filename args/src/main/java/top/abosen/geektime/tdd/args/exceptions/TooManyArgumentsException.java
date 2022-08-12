package top.abosen.geektime.tdd.args.exceptions;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */

public class TooManyArgumentsException extends RuntimeException {
    private final String option;

    public String getOption() {
        return option;
    }

    public TooManyArgumentsException(String option) {
        this.option = option;
    }
}
