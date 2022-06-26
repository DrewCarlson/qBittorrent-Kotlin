import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.binaryCompat) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

allprojects {
    yarn.lockFileDirectory = file("gradle/kotlin-js-store")
    repositories {
        mavenCentral()
    }
}
