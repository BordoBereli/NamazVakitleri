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
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":prayer:model"))
    implementation(libs.adhan)

    // Dependency Injection - Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
//    implementation(libs.koin.androidx.workmanager) // Add when WorkManager is implemented

    // --- Testing Dependencies ---
    // Standard JUnit 5 for running tests
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.koin.test.junit5)

    // Core Testing
    testImplementation(libs.kotlinx.coroutines.test) // For testing coroutines
    testImplementation(libs.turbine) // Excellent for testing Flows
    testImplementation(libs.truth) // For readable assertions
    testImplementation(libs.assertj.core) // AssertJ for more readable assertions (optional, but recommended)
    testImplementation(libs.mockk) // MockK for creating mock objects in tests

}