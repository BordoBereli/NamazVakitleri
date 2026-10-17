import java.io.FileInputStream
import java.util.Properties

// Mirrors app/build.gradle.kts: the watch artifact ships under the phone app's package
// name and must be signed with the same upload key, so it reuses the same keystore
// properties (resolved from the root project).
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
// config degrades gracefully to an unsigned bundle instead of a cryptic signing error.
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.wear"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // The watch artifact is packaged as a sibling artifact of the phone app: it must
        // share the phone app's package name for Play to attach it to the same listing.
        applicationId = "com.kutluoglu.namazvakitleri"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.wearVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()
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
            signingConfig = if (releaseSigningConfigured) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":prayer_wear_shared"))
    implementation(project(":core:common"))
    implementation(project(":prayer:domain"))

    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    implementation(libs.androidx.wear.protolayout.material3)
    implementation(libs.androidx.wear.protolayout.expression)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)

    debugImplementation(libs.androidx.wear.tiles.renderer)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.androidx.wear.tiles.testing)
}
tasks.withType<Test> { useJUnitPlatform() }
