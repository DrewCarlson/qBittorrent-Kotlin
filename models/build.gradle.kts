plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

val mavenUrl: String by ext
val mavenSnapshotUrl: String by ext

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
    //watchos()

    publishing {
        repositories {
            maven {
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    uri(mavenSnapshotUrl)
                } else {
                    uri(mavenUrl)
                }
                credentials {
                    username = System.getenv("BINTRAY_USER")
                    password = System.getenv("BINTRAY_API_KEY")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$SERIALIZATION_VERSION")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$SERIALIZATION_VERSION")
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
        val linuxX64Main by getting
        configure(listOf(win64Main, macosMain, linuxX64Main)) {
            dependsOn(desktopCommonMain)
        }

        val iosMain by getting {
            dependsOn(nativeCommonMain)
        }

        val tvosMain by getting {
            dependsOn(iosMain)
        }

        /*val watchosMain by getting {
            dependsOn(iosMain)
        }*/
    }
}
