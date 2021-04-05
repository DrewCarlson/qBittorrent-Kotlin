plugins {
    kotlin("multiplatform")
    application
}

kotlin {
    jvm {
        withJava()
    }

    macosX64("macos") {
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION")
            }
        }

        named("jvmMain") {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$KTOR_VERSION")
            }
        }

        named("macosMain") {
            dependencies {
                implementation("io.ktor:ktor-client-curl:$KTOR_VERSION")
            }
        }
    }
}

application {
    mainClass.set("demo.MainKt")
}
