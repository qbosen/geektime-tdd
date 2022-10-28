package top.abosen.geektime.tdd;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/10/19
 */
public interface Context {
    <T> T get(Type type);

    <T> Optional<T> getOpt(Type type);
}
