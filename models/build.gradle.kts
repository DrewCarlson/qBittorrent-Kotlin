import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

configure<PublishingExtension> {
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

  if (findProperty("hostPublishing") ?: "false" == "true") {
    val host = System.getProperty("os.name", "unknown")
    val publishTargets = when {
      host.contains("win", true) -> listOf(mingwX64("win64"))
      host.contains("mac", true) -> {
        val targets = mutableListOf<KotlinNativeTarget>(macosX64("macos"))
        ios { targets.add(this) }
        tvos { targets.add(this) }
        targets
      }
      else -> listOf(targets["metadata"], jvm(), js(BOTH), linuxX64())
    }.map(KotlinTarget::getName)
    publishing {
      publications {
        filter { it.name !in publishTargets }
          .forEach { publication ->
            tasks.withType<AbstractPublishToMaven>()
              .matching { it.publication == publication }
              .configureEach { enabled = false }
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
  }
}

/** Disabled all publications except for the provided [targets] */
fun exclusivePublishing(vararg targets: KotlinTarget) =
  targets.forEach { target ->
    target.mavenPublication {
      val targetPublication = this@mavenPublication
      tasks.withType<AbstractPublishToMaven>()
        .matching { it.publication != targetPublication }
        .all { enabled = false }
    }
  }
