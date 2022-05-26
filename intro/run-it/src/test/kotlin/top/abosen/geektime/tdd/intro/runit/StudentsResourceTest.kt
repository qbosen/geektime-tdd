package top.abosen.geektime.tdd.intro.runit

import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.Response
import org.glassfish.jersey.test.JerseyTest
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

internal class StudentsResourceTest : JerseyTest() {
    override fun configure(): Application {
        return top.abosen.geektime.tdd.intro.runit.Application()
    }

    @Test
    fun `should fetch all students from api`() {
        val students = target("students").request().get(Array<Student>::class.java)
        assertEquals(3, students.size)

        assertEquals("John", students[0].firstName)
        assertEquals("Smith", students[0].lastName)
        assertEquals("john.smith@email.com", students[0].email)
        assertEquals(1, students[0].id)
    }

    @Test
    fun `should be able fetch student by id`() {
        val students = target("students/1").request().get(Student::class.java)

        assertEquals("John", students.firstName)
        assertEquals("Smith", students.lastName)
        assertEquals("john.smith@email.com", students.email)
        assertEquals(1, students.id)
    }

    @Test
    fun `should return 404 if no student found`() {
        val response = target("students/4").request().get(Response::class.java)
        assertEquals(Response.Status.NOT_FOUND.statusCode , response.status)
    }
}