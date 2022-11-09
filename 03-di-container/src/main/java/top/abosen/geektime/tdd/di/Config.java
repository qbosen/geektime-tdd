package top.abosen.geektime.tdd.di;

import java.lang.annotation.*;

/**
 * @author qiubaisen
 * @date 2022/11/9
 */
public interface Config {

    /**
     * TODO 用于 DSL
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Export {
        Class<?> value();
    }
}
