package top.abosen.geektime.tdd.di;

import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/11/8
 */
interface ComponentProvider<T> {
    T get(Context context);

    default List<ComponentRef<?>> getDependencies() {
        return List.of();
    }
}
