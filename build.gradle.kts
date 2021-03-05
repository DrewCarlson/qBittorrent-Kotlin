plugins {
    kotlin("multiplatform") version KOTLIN_VERSION apply false
    kotlin("plugin.serialization") version KOTLIN_VERSION apply false
    id("org.jetbrains.dokka") version "1.4.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
