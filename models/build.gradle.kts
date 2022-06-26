plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

apply(from = "$rootDir/gradle/publishing.gradle.kts")

kotlin {
    jvm()
    js(IR) {
        browser()
        nodejs()
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
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }

        val nativeCommonMain by creating {
            dependsOn(commonMain)
            dependencies {
            }
        }
        val desktopCommonMain by creating {
            dependsOn(nativeCommonMain)
        }

        val win64Main by getting
        val macosMain by getting
        val macosArm64Main by getting
        val linuxX64Main by getting
        configure(listOf(win64Main, macosMain, macosArm64Main, linuxX64Main)) {
            dependsOn(desktopCommonMain)
        }

        val iosMain by getting {
            dependsOn(nativeCommonMain)
        }

        val tvosMain by getting
        val tvosArm64Main by getting
        val watchosArm32Main by getting
        val watchosArm64Main by getting
        val watchosX86Main by getting
        val watchosSimulatorArm64Main by getting
        val iosSimulatorArm64Main by getting

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
    }
}
