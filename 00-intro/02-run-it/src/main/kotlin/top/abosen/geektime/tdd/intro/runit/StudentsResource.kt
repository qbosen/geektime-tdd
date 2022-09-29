package top.abosen.geektime.tdd.intro.runit

import jakarta.inject.Singleton
import jakarta.persistence.Persistence
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/students")
@Singleton
class StudentsResource {
    private val repository: StudentRepository

    // 不熟悉 jax-rs 相关，这里直接注入repository 并初始化相关数据
    companion object {
        // 在test中 容器会启动多次，但是我们希望数据只初始化一次
        var initialized = false
    }

    init {
        val factory = Persistence.createEntityManagerFactory("student")
        val entityManager = factory.createEntityManager()
        repository = StudentRepository(entityManager)

        if (!initialized) {
            repository.manager.transaction.begin()
            repository.save(Student("John", "Smith", "john.smith@email.com"))
            repository.save(Student("Henry", "Ford", "henry.ford@email.com"))
            repository.save(Student("Adam", "Evans", "adam.evans@email.com"))
            repository.manager.transaction.commit()
            initialized = true
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun all(): List<Student> = repository.all()

    @GET
    @Path(("{id}"))
    @Produces(MediaType.APPLICATION_JSON)
    fun findById(@PathParam("id") id: Long): Response = repository.findById(id)
        ?.let { Response.ok(it).build() }
        ?: Response.status(Response.Status.NOT_FOUND).build()
}