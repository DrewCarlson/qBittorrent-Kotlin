@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvm()
    js(IR) {
        browser {
            testTask {
                useMocha {
                    timeout = "10000"
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "10000"
                }
            }
        }
    }
    macosX64("macos")
    macosArm64()
    mingwX64("win64")
    linuxX64()

    ios()
    iosSimulatorArm64()
    tvos()
    tvosArm64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":qbittorrent-models"))
                implementation(libs.coroutines.core)
                implementation(libs.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.coroutines.test)
                implementation(libs.coroutines.turbine)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(kotlin("test-js"))
            }
        }

        val nativeCommonMain by creating {
            dependsOn(commonMain)
        }

        val darwinMain by creating {
            dependsOn(nativeCommonMain)
        }
        val darwinTest by creating {
            dependsOn(nativeCommonMain)
            dependsOn(commonTest)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val win64Main by getting
        val win64Test by getting {
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
        val macosMain by getting {
            dependsOn(darwinMain)
        }
        val macosTest by getting
        val macosArm64Main by getting
        val macosArm64Test by getting
        val linuxX64Main by getting
        val linuxX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        configure(listOf(win64Main, macosMain, macosArm64Main, linuxX64Main)) {
            dependsOn(nativeCommonMain)
        }

        val tvosMain by getting
        val tvosTest by getting
        val tvosArm64Main by getting
        val tvosArm64Test by getting
        val watchosArm32Main by getting
        val watchosArm32Test by getting
        val watchosArm64Main by getting
        val watchosArm64Test by getting
        val watchosX86Main by getting
        val watchosX86Test by getting
        val watchosSimulatorArm64Main by getting
        val watchosSimulatorArm64Test by getting
        val iosMain by getting
        val iosTest by getting
        val iosSimulatorArm64Main by getting
        val iosSimulatorArm64Test by getting

        configure(
            listOf(
                iosMain,
                tvosMain,
                tvosArm64Main,
                macosMain,
                macosArm64Main,
                watchosArm32Main,
                watchosArm64Main,
                watchosX86Main,
                watchosSimulatorArm64Main,
                iosSimulatorArm64Main,
            )
        ) {
            dependsOn(darwinMain)
        }
        configure(
            listOf(
                iosTest,
                tvosTest,
                tvosArm64Test,
                macosTest,
                macosArm64Test,
                watchosArm32Test,
                watchosArm64Test,
                watchosX86Test,
                watchosSimulatorArm64Test,
                iosSimulatorArm64Test,
            )
        ) {
            dependsOn(darwinTest)
        }
    }
}
