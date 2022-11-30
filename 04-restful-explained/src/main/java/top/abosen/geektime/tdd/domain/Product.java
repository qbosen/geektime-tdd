package top.abosen.geektime.tdd.domain;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public class Product {
    public record Id(long value) {
    }

    protected Id id;

    public Product(Id id) {
        this.id = id;
    }

    public Id getId() {
        return id;
    }
}
