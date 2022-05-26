plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}
group = "top.abosen.geektime.tdd.intro"
version = "1.0-SNAPSHOT"

dependencies{
    implementation(Deps.jakartaPersistence)
    implementation(Deps.hibernate)
    implementation(Deps.h2)
}

noArg{
    annotation("jakarta.persistence.Entity")
}