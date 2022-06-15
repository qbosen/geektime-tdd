package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class IllegalOptionException extends RuntimeException {
    private final String parameter;

    public IllegalOptionException(String parameter) {
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }
}
