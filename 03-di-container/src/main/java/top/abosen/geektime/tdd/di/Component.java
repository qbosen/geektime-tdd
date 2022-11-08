package top.abosen.geektime.tdd.di;

import java.lang.annotation.Annotation;

/**
 * @author qiubaisen
 * @date 2022/11/3
 */
public record Component(Class<?> type, Annotation qualifier) {
}
