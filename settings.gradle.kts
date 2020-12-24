pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "qBittorrent"

include(":client", ":models")

enableFeaturePreview("GRADLE_METADATA")
