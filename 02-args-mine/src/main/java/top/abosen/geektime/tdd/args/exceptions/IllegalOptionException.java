package top.abosen.geektime.tdd.args.exceptions;

/**
 * @author qiubaisen
 * @date 2022/9/3
 */
public class IllegalOptionException extends RuntimeException{
    private final String parameter;

    public IllegalOptionException(String parameter) {
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
