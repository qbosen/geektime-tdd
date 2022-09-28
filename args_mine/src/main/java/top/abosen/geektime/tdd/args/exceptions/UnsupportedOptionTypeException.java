package top.abosen.geektime.tdd.args.exceptions;

/**
 * @author qiubaisen
 * @date 2022/9/3
 */
public class UnsupportedOptionTypeException extends RuntimeException {
    private final String type;


    public String getType() {
        return type;
    }

    public UnsupportedOptionTypeException(String type) {
        this.type = type;
    }
}
