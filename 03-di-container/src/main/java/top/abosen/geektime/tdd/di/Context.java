package top.abosen.geektime.tdd.di;

import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
public interface Context {
    <T> T get(ComponentRef<T> ref);

    <T> Optional<T> getOpt(ComponentRef<T> ref);

}
