import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.binaryCompat) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.mavenPublish)
}


allprojects {
    version = System.getenv("GITHUB_REF")?.substringAfter("refs/tags/v", version.toString()) ?: version

    repositories {
        mavenCentral()
    }

    plugins.withType<NodeJsRootPlugin> {
        the<YarnRootExtension>().lockFileDirectory = rootDir.resolve("gradle/kotlin-js-store")
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
