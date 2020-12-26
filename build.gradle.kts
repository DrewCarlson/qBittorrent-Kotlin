plugins {
    kotlin("multiplatform") version KOTLIN_VERSION apply false
    kotlin("plugin.serialization") version KOTLIN_VERSION apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}

System.getenv("GITHUB_REF")?.let { ref ->
    if (ref.split('/').getOrNull(1)?.contains("tag") == true) {
        version = (version as String).substringBefore("-SNAPSHOT")
    }
}
