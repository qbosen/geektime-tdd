package top.abosen.geektime.tdd.domain;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public class User {
    public static record Id(long value) {
    }

    protected Id id;
    private String email;
    private String name;

    public User(Id id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

}
