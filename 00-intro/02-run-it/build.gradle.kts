plugins {
    id("org.jetbrains.kotlin.plugin.jpa")
}
group = "top.abosen.geektime.tdd.intro"
version = "1.0-SNAPSHOT"

dependencies{
    implementation(Deps.jakartaPersistence)
    implementation(Deps.jakartaRS)
    implementation(Deps.jakartaInject)
    implementation(Deps.jakartaServletApi)
    implementation(Deps.jakartaEL)

    implementation(Deps.jerseyJettyHttp)
    implementation(Deps.jerseyInject)
    implementation(Deps.jerseyJackson)
    implementation(Deps.glassfishJakartaEL)
    testImplementation(Deps.jerseyTest)
    testImplementation(Deps.jerseyTestProvider)

    implementation(Deps.hibernate)
    implementation(Deps.hibernateValidator)
    implementation(Deps.h2)


}

noArg{
    annotation("jakarta.persistence.Entity")
}