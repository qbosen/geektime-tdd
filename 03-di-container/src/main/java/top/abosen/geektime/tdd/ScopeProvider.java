package top.abosen.geektime.tdd;

/**
 * @author qiubaisen
 * @date 2022/11/8
 */
interface ScopeProvider {
    ComponentProvider<?> create(ComponentProvider<?> provider);
}
