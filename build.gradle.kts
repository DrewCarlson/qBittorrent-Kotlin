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

