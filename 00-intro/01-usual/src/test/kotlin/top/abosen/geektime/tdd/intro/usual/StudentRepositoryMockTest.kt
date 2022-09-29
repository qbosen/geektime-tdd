package top.abosen.geektime.tdd.intro.usual

import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import kotlin.test.Test

internal class StudentRepositoryMockTest {
    private lateinit var manager: EntityManager
    private lateinit var repository: StudentRepository
    private val john: Student = Student("john", "smith", "john.smith@email.com")

    @BeforeEach
    fun setUp() {
        manager = mock(EntityManager::class.java)
        repository = StudentRepository(manager)
    }

    @Test
    fun `should generate id for saved entity`() {
        repository.save(john)
        verify(manager).persist(john)
    }

    @Test
    fun `should be able to load saved student by id`() {
        `when`(manager.find(any() as? Class<*>, any())).thenReturn(john)

        val loaded = repository.findById(1L)
        assertEquals(john, loaded)
        verify(manager).find(Student::class.java, 1L)
    }

    @Test
    fun `should be able to load saved student by email`() {
        val query = mock(TypedQuery::class.java)

        `when`(manager.createQuery(any(), any() as? Class<*>)).thenReturn(query)
        `when`(query.setParameter(any(String::class.java), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(john))

        val loaded = repository.findByEmail(john.email)
        assertEquals(john, loaded)

        verify(manager).createQuery("select s from Student s where s.email = :email", Student::class.java)
        verify(query).setParameter("email", john.email)
    }
}