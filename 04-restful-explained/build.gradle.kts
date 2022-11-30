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
    implementation("org.springframework.hateoas:spring-hateoas:2.0.0")

    implementation("org.glassfish.jersey.containers:jersey-container-jetty-http:3.1.0")
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.0")
    implementation("org.glassfish.jersey.media:jersey-media-json-jackson:3.1.0")

    testImplementation("org.glassfish.jersey.core:jersey-client:3.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-jetty:3.1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java{
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}