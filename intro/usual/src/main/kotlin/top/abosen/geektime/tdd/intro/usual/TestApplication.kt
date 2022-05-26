package top.abosen.geektime.tdd.intro.usual

import jakarta.persistence.Persistence

fun main() {
    val factory = Persistence.createEntityManagerFactory("student")
    val entityManager = factory.createEntityManager()
    entityManager.transaction.begin()
    val repository = StudentRepository(entityManager)
    val john=repository.save(Student("john", "smith","john.smith@email.com"))
    entityManager.transaction.commit()

    println(john.id)
    println(repository.findById(john.id))

    println(repository.findByEmail("john.smith@email.com"))
    println(repository.findByEmail("john.smith@email1.com"))
}
