import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    application
}

kotlin {
    jvm {
        withJava()
    }

    mingwX64("windows")
    macosX64("macos")

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
            }
        }

        named("jvmMain") {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        named("macosMain") {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        named("windowsMain") {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
    }
}

application {
    mainClass.set("demo.MainKt")
}
