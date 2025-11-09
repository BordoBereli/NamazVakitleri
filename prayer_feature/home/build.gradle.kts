import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.prayer_feature.home"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    //region --- Project Dependencies ---
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":prayer:domain"))
    implementation(project(":prayer:data"))
    implementation(project(":prayer_navigation:core"))
    implementation(project(":prayer_location"))
    //endregion

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    //region Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons.extended)
    //endregion

    implementation(libs.compose.navigation)

    implementation(libs.kotlinx.datetime)

    //region Handle Permission
    implementation(libs.accompanist.permissions)
    //endregion
    //region Coil
    implementation(libs.coil)
    //endregion
    //region Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)
    //endregion

    //region --- Default Testing Dependencies ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //endregion

    //region --- Testing Dependencies ---
    //region Standard JUnit 5 for running tests
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.koin.test.junit5)
    //endregion
    //region Core Testing
    testImplementation(libs.kotlinx.coroutines.test) // For testing coroutines
    testImplementation(libs.turbine) // Excellent for testing Flows
    testImplementation(libs.truth) // For readable assertions
    testImplementation(libs.mockk) // MockK for creating mock objects in tests
    testImplementation(libs.assertj.core) // AssertJ for more readable assertions (optional, but recommended)
    //endregion
    //region Test Suite
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)
    //endregion
    //region Koin Testing
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
    //endregion
    //endregion --- Testing Dependencies ---
}
tasks.withType<Test> { useJUnitPlatform() }