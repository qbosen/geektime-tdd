plugins {
    `java-library`
    "jacoco"
}

group = "top.abosen.geektime.tdd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    implementation("org.eclipse.jetty:jetty-servlet:11.0.12")
    implementation("org.eclipse.jetty:jetty-server:11.0.12")

    implementation("top.abosen.geektime.tdd:container:1.0-SNAPSHOT")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("org.slf4j:slf4j-simple:2.0.5")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java{
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}