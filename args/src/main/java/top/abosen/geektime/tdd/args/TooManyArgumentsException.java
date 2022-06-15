package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */

public class TooManyArgumentsException extends RuntimeException {
    String option;

    public String getOption() {
        return option;
    }

    public TooManyArgumentsException(String option) {
        this.option = option;
    }
}
