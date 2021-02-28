pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "qBittorrent"

include(":client", ":models", ":demo")

enableFeaturePreview("GRADLE_METADATA")
