plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    sourceSets.all {
        languageSettings {
            optIn("com.google.devtools.ksp.KspExperimental")
        }
    }
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.compileTesting)
    implementation(libs.compileTesting.ksp)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}