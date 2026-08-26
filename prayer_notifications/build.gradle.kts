import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.prayer_notifications"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":prayer:model"))
    implementation(project(":prayer:domain"))
    implementation(project(":prayer:data"))
    implementation(project(":prayer_location"))
    implementation(project(":prayer_settings"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)

    api(platform(libs.koin.bom))
    api(libs.koin.core)
    api(libs.koin.annotations)
    implementation(libs.koin.android)
    ksp(libs.koin.ksp)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation("androidx.test:core:1.7.0")
    testImplementation(libs.koin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
tasks.withType<Test> { useJUnitPlatform() }
