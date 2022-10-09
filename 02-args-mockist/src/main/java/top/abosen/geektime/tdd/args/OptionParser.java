package top.abosen.geektime.tdd.args;

/**
 * @author qiubaisen
 * @date 2022/10/8
 */
public interface OptionParser {
   <T> T parse(Class<T> type, String[] optVals);
}
