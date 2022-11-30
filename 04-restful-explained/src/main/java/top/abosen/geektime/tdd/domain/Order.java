package top.abosen.geektime.tdd.domain;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public class Order {
    public record Id(long value){}

    protected Id id;

    public Order(Id id) {
        this.id = id;
    }

    public Id getId() {
        return id;
    }


}
