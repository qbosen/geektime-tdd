package top.abosen.geektime.tdd.intro.usual

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

internal class StudentRepositoryTest {
    private lateinit var factory: EntityManagerFactory
    private lateinit var manager: EntityManager
    private lateinit var repository: StudentRepository
    private lateinit var john: Student

    @BeforeEach
    fun setUp() {
        factory = Persistence.createEntityManagerFactory("student")
        manager = factory.createEntityManager()
        repository = StudentRepository(manager)

        manager.transaction.begin()
        john = repository.save(Student("john", "smith", "john.smith@email.com"))
        manager.transaction.commit()

    }

    @AfterEach
    fun tearDown() {
        manager.clear()
        manager.close()
        factory.close()
    }

    @Test
    fun `should generate id for saved entity`() {
        assertNotEquals(0, john.id)
    }

    @Test
    fun `should be able to load saved student by id`() {
        val loaded = repository.findById(john.id)
        assertNotNull(loaded)
        loaded!!
        assertEquals(john.firstName, loaded.firstName)
        assertEquals(john.lastName, loaded.lastName)
        assertEquals(john.email, loaded.email)
        assertEquals(john.id, loaded.id)
    }

    @Test
    fun `should be able to load saved student by email`() {
        val loaded = repository.findByEmail(john.email)

        assertNotNull(loaded)
        loaded!!

        assertEquals(john.firstName, loaded.firstName)
        assertEquals(john.lastName, loaded.lastName)
        assertEquals(john.email, loaded.email)
        assertEquals(john.id, loaded.id)
    }
}