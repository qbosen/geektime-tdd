package top.abosen.geektime.tdd.di;

import java.lang.annotation.*;

/**
 * @author qiubaisen
 * @date 2022/11/9
 */
public interface Config {

    /**
     * 用于 DSL 中 标准该类型的注册类型
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Export {
        Class<?> value();
    }
}
