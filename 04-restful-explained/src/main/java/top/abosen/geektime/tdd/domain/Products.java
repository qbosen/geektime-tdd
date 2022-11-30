package top.abosen.geektime.tdd.domain;

import java.util.List;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public interface Products {
    List<Product> find(List<Product.Id> ids);
}
