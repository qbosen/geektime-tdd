package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/10/8
 */
public interface ValueRetriever {
    String[] getValues(String name, String[] args);
}
