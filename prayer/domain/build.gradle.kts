plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies {
    // This module needs the data models (e.g., the Prayer data class)
    implementation(project(":prayer:model"))
    implementation(libs.adhan)

    // Dependency Injection - Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.core.viewmodel)
//    implementation(libs.koin.androidx.workmanager) // Add when WorkManager is implemented

    // --- Testing Dependencies ---
    // Standard JUnit 5 for running tests
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.jupiter.junit.jupiter.engine)

    // MockK for creating mock objects in tests
    testImplementation(libs.mockk)

    // AssertJ for more readable assertions (optional, but recommended)
    testImplementation(libs.assertj.core)

    // For testing coroutines if your service uses them
    testImplementation(libs.kotlinx.coroutines.test)


    // Core Testing
    testImplementation(platform { libs.junit.bom })
    testImplementation(libs.kotlinx.coroutines.test) // For testing coroutines
    testImplementation(libs.turbine) // Excellent for testing Flows
    testImplementation(libs.truth) // For readable assertions
//    testImplementation(libs.junit.jupiter.api)
//    testImplementation(libs.junit.jupiter.engine)
//    testImplementation(libs.junit.jupiter.params)
//    testImplementation(libs.jupiter.junit.jupiter.engine)
    testImplementation(libs.assertj.core)

}

// This block is still required to tell Gradle to use the JUnit 5 runner
tasks.withType<Test> {
    useJUnitPlatform()
}