package top.abosen.geektime.tdd;

import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
public interface Context {
    <Type> Type get(Class<Type> type);

    <Type> Optional<Type> getOpt(Class<Type> type);
}
