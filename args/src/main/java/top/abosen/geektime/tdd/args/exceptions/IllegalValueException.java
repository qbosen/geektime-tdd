package top.abosen.geektime.tdd.args.exceptions;

/**
 * @author qiubaisen
 * @date 2022/9/3
 */
public class IllegalValueException extends RuntimeException{
    private final String option;
    private final String value;

    public String getOption() {
        return option;
    }

    public String getValue() {
        return value;
    }

    public IllegalValueException(String option, String value) {
        this.option = option;
        this.value = value;
    }
}
