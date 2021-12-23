import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform") version KOTLIN_VERSION apply false
    kotlin("plugin.serialization") version KOTLIN_VERSION apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version BINARY_COMPAT_VERSION apply false
    id("org.jetbrains.dokka") version DOKKA_VERSION
}

allprojects {
    yarn.lockFileDirectory = file("gradle/kotlin-js-store")
    repositories {
        mavenCentral()
    }
}
