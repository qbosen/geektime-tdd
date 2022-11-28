package top.abosen.geektime.tdd.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.hateoas.Link;
import top.abosen.geektime.tdd.domain.User;
import top.abosen.geektime.tdd.domain.Users;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
@Path("/users")
public class UsersResource {
    @Context
    private Users users;

    @Path("{id}")
    public UserResource findById(@PathParam("id") User.Id id) {
        return users.findById(id).map(UserResource::new).orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@FormParam("name") String name, @FormParam("email") String email) {
        User user = users.create(name, email);
        return Response.created(Link.of("/users/{id}").expand(user.getId().value()).toUri()).build();
    }


    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(@BeanParam UserRegistrationForm form) {
        User user = users.create(form.name, form.email);
        return Response.created(Link.of("/users/{id}").expand(user.getId().value()).toUri()).build();
    }


    static class UserRegistrationForm {
        @FormParam("name")
        public String name;
        @FormParam("email")
        public String email;
    }
}
