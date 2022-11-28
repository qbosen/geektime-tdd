package top.abosen.geektime.tdd.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import top.abosen.geektime.tdd.domain.User;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
public class UserResource {
    private User user;

    public UserResource(User user) {
        this.user = user;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EntityModel<User> get() {
        return EntityModel.of(user).add(Link.of("/users/{value}").expand(user.getId().value()));
    }

    @Path("/orders")
    public UserOrdersResource orders(@Context ResourceContext context) {
        return context.initResource(new UserOrdersResource(user));
    }
}
