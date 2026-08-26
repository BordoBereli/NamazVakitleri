import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kutluoglu.prayer_feature.prayertimes"
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
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":prayer:domain"))
    implementation(project(":prayer:model"))
    implementation(project(":prayer_navigation:core"))
    implementation(project(":prayer_feature:common"))
    implementation(project(":prayer_location"))
    implementation(project(":prayer_settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.compose.navigation)

    implementation(libs.kotlinx.datetime)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.android.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //region --- Compose UI Testing (Robolectric) ---
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit.vintage.engine)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    //endregion

    configurations.all {
        resolutionStrategy {
            force(
                "androidx.test:core:1.7.0",
                "androidx.test.ext:junit:1.3.0",
                "androidx.test.espresso:espresso-core:3.7.0",
                "androidx.test:monitor:1.8.0",
                "androidx.test:runner:1.7.0"
            )
        }
    }

    //region --- Testing Dependencies ---
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.platform.junit.platform.suite.engine)
    //endregion
}
tasks.withType<Test> { useJUnitPlatform() }
