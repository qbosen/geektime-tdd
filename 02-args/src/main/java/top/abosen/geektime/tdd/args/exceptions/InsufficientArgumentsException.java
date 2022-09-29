package top.abosen.geektime.tdd.args.exceptions;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class InsufficientArgumentsException extends RuntimeException {
    private final String option;

    public String getOption() {
        return option;
    }

    public InsufficientArgumentsException(String option) {
        this.option = option;
    }
}
