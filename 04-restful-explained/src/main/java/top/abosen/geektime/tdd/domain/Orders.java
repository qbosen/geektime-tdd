package top.abosen.geektime.tdd.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public interface Orders {
    List<Order> findBy(User.Id id);

    Optional<Order> findBy(User.Id userId, Order.Id id);

    Order create(User user, Map<Product, Double> orderItems);
}
