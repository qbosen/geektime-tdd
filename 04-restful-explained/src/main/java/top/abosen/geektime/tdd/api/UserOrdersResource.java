package top.abosen.geektime.tdd.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import top.abosen.geektime.tdd.domain.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserOrdersResource {
    private User user;
    @Context
    private Orders orders;
    @Context
    private Products products;
    public UserOrdersResource(User user) {
        this.user = user;
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CollectionModel<Order> all() {
        return CollectionModel.of(orders.findBy(user.getId()))
                .add(Link.of("/users/{id}/orders").expand(user.getId().value()),
                        Link.of("/users/{id}", "owner").expand(user.getId().value()));
    }
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public EntityModel<Order> findBy(@PathParam("id") long id) {
        return orders.findBy(user.getId(), new Order.Id(id))
                .map(o -> EntityModel.of(o)
                        .add(Link.of("/users/{id}/orders/{id}").expand(user.getId().value(), id),
                                Link.of("/users/{id}", "owner").expand(user.getId().value())))
                .orElse(null);
    }
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response placeOrder(@FormParam("item") List<Long> items, @FormParam("quantity") List<Double> quantities) {
        List<Product> products = this.products.find(items.stream().map(Product.Id::new).toList());
        Map<Product, Double> orderItems = new HashMap<>();
        for (int i = 0; i < products.size(); i++)
            orderItems.put(products.get(i), quantities.get(i));
        Order order = orders.create(user, orderItems);
        return Response.created(Link.of("/users/{uid}/orders/{oid}").expand(user.getId().value(), order.getId().value()).toUri()).build();
    }
}
    
