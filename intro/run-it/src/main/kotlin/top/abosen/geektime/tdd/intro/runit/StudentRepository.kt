package top.abosen.geektime.tdd.intro.runit

import jakarta.persistence.EntityManager

class StudentRepository(
    val manager: EntityManager
) {
    fun save(student: Student): Student = student.also { manager.persist(it) }

    fun findById(id: Long): Student? = manager.find(Student::class.java, id)

    fun findByEmail(email: String): Student? {
        val query = manager.createQuery("select s from Student s where s.email = :email", Student::class.java)
        return query.setParameter("email", email).resultList.firstOrNull()
    }

    fun all(): List<Student> {
        val query = manager.createQuery("select s from Student s", Student::class.java)
        return query.resultList
    }
}