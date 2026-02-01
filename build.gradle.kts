plugins {
    kotlin("jvm") version "2.3.0"
    application
}

group = "net.exoad"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("net.exoad.k.MainKt")
}

tasks.test {
    useJUnitPlatform()
}