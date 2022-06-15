package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class InsufficientArgumentsException extends RuntimeException {
    private String option;

    public String getOption() {
        return option;
    }

    public InsufficientArgumentsException(String option) {
        this.option = option;
    }
}
