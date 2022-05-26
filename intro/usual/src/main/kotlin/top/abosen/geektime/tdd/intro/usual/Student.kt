package top.abosen.geektime.tdd.intro.usual

import jakarta.persistence.*

@Entity
@Table(name = "STUDENTS")
data class Student(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    var firstName: String,
    var lastName: String,
    var email: String
) {
    constructor(
        firstName: String,
        lastName: String,
        email: String
    ) : this(0, firstName, lastName, email)
}