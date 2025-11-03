import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.prayer_location"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
        jvmToolchain(8)
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":prayer:model"))
    implementation(project(":prayer:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)

    // Dependency Injection - Koin
    api(platform(libs.koin.bom))
    api(libs.koin.core)
    api(libs.koin.annotations)
    ksp(libs.koin.ksp)

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
    testImplementation(libs.mockk) // MockK for creating mock objects in tests
    testImplementation(libs.assertj.core) // AssertJ for more readable assertions (optional, but recommended)

    // Test Suite
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}
tasks.withType<Test> { useJUnitPlatform() }