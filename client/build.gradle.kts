plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

apply(from = "$rootDir/gradle/publishing.gradle.kts")

kotlin {
    jvm()
    js(BOTH) {
        browser()
        nodejs()
    }
    macosX64("macos")
    mingwX64("win64")
    linuxX64()

    ios()
    tvos()
    watchosArm32()
    watchosArm64()
    watchosX86()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":qbittorrent-models"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
                implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-json:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-serialization:$KTOR_VERSION")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }

        val nativeCommonMain by creating {
            dependsOn(commonMain)
        }
        val desktopCommonMain by creating {
            dependsOn(nativeCommonMain)
        }

        val win64Main by getting
        val macosMain by getting
        val linuxX64Main by getting
        configure(listOf(win64Main, macosMain, linuxX64Main)) {
            dependsOn(desktopCommonMain)
        }

        val iosMain by getting {
            dependsOn(nativeCommonMain)
        }

        val watchosArm32Main by getting
        val watchosArm64Main by getting
        val watchosX86Main by getting

        configure(listOf(tvosMain, watchosArm32Main, watchosArm64Main, watchosX86Main)) {
            dependsOn(iosMain)
        }
    }
}
