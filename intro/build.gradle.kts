import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
}

group = "top.abosen.geektime.tdd.intro"
version = "1.0-SNAPSHOT"

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation(kotlin("test"))
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

}

subprojects{
    task("wrapper") {}
    task("prepareKotlinBuildScriptModel") {}
}