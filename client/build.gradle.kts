plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

apply(from = "$rootDir/gradle/publishing.gradle.kts")

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
        val nativeCommonTest by creating {
            dependsOn(commonTest)
        }
        val desktopCommonMain by creating {
            dependsOn(nativeCommonMain)
        }
        val desktopCommonTest by creating {
            dependsOn(nativeCommonTest)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val darwinMain by creating {
            dependsOn(commonMain)
        }

        val win64Main by getting
        val win64Test by getting
        val macosMain by getting {
            dependsOn(darwinMain)
        }
        val macosTest by getting
        val macosArm64Main by getting {
            dependsOn(macosMain)
        }
        val macosArm64Test by getting
        val linuxX64Main by getting
        val linuxX64Test by getting
        configure(listOf(win64Main, macosMain, macosArm64Main, linuxX64Main)) {
            dependsOn(desktopCommonMain)
        }
        configure(listOf(win64Test, macosTest, macosArm64Test, linuxX64Test)) {
            dependsOn(desktopCommonTest)
        }

        val iosMain by getting {
            dependsOn(darwinMain)
            dependsOn(nativeCommonMain)
        }
        val iosTest by getting {
            dependsOn(darwinMain)
            dependsOn(nativeCommonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
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
        val iosSimulatorArm64Main by getting
        val iosSimulatorArm64Test by getting

        configure(
            listOf(
                tvosMain,
                tvosArm64Main,
                watchosArm32Main,
                watchosArm64Main,
                watchosX86Main,
                watchosSimulatorArm64Main,
                iosSimulatorArm64Main,
            )
        ) {
            dependsOn(iosMain)
        }
        configure(
            listOf(
                tvosTest,
                tvosArm64Test,
                watchosArm32Test,
                watchosArm64Test,
                watchosX86Test,
                watchosSimulatorArm64Test,
                iosSimulatorArm64Test,
            )
        ) {
            dependsOn(iosTest)
        }
    }
}
