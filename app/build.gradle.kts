import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun keystoreProp(propName: String, envName: String): String? =
    keystoreProperties.getProperty(propName) ?: System.getenv(envName)

val releaseStoreFile = keystoreProp("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = keystoreProp("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = keystoreProp("keyAlias", "KEYSTORE_KEY_ALIAS")
val releaseKeyPassword = keystoreProp("keyPassword", "KEYSTORE_KEY_PASSWORD")

// Only sign the release build when the full signing config is present; a partial
// config degrades gracefully to an unsigned APK instead of a cryptic signing error.
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.gradle)
}

android {
    namespace = "com.kutluoglu.namazvakitleri"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kutluoglu.namazvakitleri"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Resolve relative to the repo root, matching where keystore.properties lives.
            storeFile = releaseStoreFile?.let { rootProject.file(it) }
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
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
    ksp {
        arg("KOIN_DEFAULT_MODULE", "true")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":prayer:domain"))
    implementation(project(":prayer:data"))
    implementation(project(":prayer_navigation:core"))
    implementation(project(":prayer_feature:home"))
    implementation(project(":prayer_feature:prayertimes"))
    implementation(project(":prayer_feature:qibla"))
    implementation(project(":prayer_feature:common"))
    implementation(project(":prayer_feature:settings"))
    implementation(project(":prayer_settings"))
    implementation(project(":prayer_location"))
    implementation(project(":prayer_qibla"))
    implementation(project(":prayer_notifications"))
    implementation(project(":prayer_remote"))
    implementation(project(":app_update"))
    implementation(project(":prayer_widget"))
    implementation(libs.play.services.wearable)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.compose.navigation)

    implementation(libs.kotlinx.datetime)

    // Firebase
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)
    implementation(libs.koin.annotations)
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

    // Koin Testing
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
tasks.withType<Test> { useJUnitPlatform() }