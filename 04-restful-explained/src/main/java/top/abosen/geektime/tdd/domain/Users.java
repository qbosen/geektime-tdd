package top.abosen.geektime.tdd.domain;

import java.util.Optional;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public interface Users {
    Optional<User> findById(User.Id id);

    User create(String name, String email);
}
