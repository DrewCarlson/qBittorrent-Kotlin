import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.binaryCompat) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

allprojects {
    yarn.lockFileDirectory = file("gradle/kotlin-js-store")
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    extensions.findByType<com.diffplug.gradle.spotless.SpotlessExtension>()?.apply {
        kotlin {
            target("**/**.kt")
            ktlint(libs.versions.ktlint.get())
                .setUseExperimental(true)
                .editorConfigOverride(mapOf(
                    "enabled_rules" to "",
                    "disabled_rules" to "no-wildcard-imports,no-unused-imports,trailing-comma"
                ))
        }
    }
}