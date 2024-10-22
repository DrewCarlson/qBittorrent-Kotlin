import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.ksp)
}

dependencies {
    add("kspCommonMainMetadata", project(":serialname-codegen"))
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    macosX64()
    macosArm64()
    mingwX64()
    linuxX64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.serialization.core)
                implementation(libs.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
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
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<AbstractArchiveTask>().all {
    if (name.endsWith("SourcesJar")) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
