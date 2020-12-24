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

group = "drewcarlson"
version = "0.0.1"

allprojects {
  repositories {
    mavenCentral()
    jcenter()
  }
}

