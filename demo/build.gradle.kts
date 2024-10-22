import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("demo.MainKt")
        }
    }

    mingwX64("windows")
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable {
                entryPoint("demo.main")
            }
        }
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":qbittorrent-client"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.coroutines.core)
                implementation(libs.io)
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        named("appleMain") {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        named("windowsMain") {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
    }
}
