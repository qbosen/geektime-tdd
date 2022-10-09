plugins {
    id("java")
}

group = "top.abosen.geektime.tdd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:4.8.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}