plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
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
    watchos()

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
                implementation(project(":models"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION")
                implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-json:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-serialization:$KTOR_VERSION")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("io.ktor:ktor-client-core-jvm:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-json-jvm:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-serialization-jvm:$KTOR_VERSION")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core-js:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-json-js:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-serialization-js:$KTOR_VERSION")
            }
        }

        val nativeCommonMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-core:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-json:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-serialization:$KTOR_VERSION")
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

        val watchosMain by getting {
            dependsOn(iosMain)
        }
    }
}
