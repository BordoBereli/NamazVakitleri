pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NamazVakitleri"
include(":app")
// Foundation Modules
include(":core:common")          // Shared utilities, extensions, base classes
include(":core:ui")              // Reusable composables/views - MaterialTheme, colors, typography
include(":prayer_navigation:core") // Navigation setup, destinations
//include(":core:database")        // Room database, DAOs
//include(":core:preferences")     // DataStore/SharedPreferences wrapper
//include(":core:di")              // Dependency injection setup (Hilt/Koin)

// Domain Modules
include(":prayer:domain") // Prayer calculations, business logic
include(":prayer:model")  // Data classes
include(":prayer:data")   // Repository implementations
include(":prayer_cache")  // Local data sources
include(":prayer_remote") // Remote data sources, Retrofit, API interfaces, network utils

// Feature-specific Core
include(":prayer_location") // Location services wrapper
//include(":core:qibla")           // Qibla direction calculations


// Features
//include(":feature:home")
//include(":feature:prayertimes")
//include(":feature:qibla")
//include(":feature:settings")
include(":prayer_feature:common")
include(":prayer_feature:home")
include(":prayer_feature:prayertimes")

