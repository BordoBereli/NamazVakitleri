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
//include(":core:network")         // Retrofit, API interfaces, network utils
//include(":core:database")        // Room database, DAOs
//include(":core:preferences")     // DataStore/SharedPreferences wrapper
//include(":core:di")              // Dependency injection setup (Hilt/Koin)

// Domain Modules
//include(":core:model")           // Data classes, entities
//include(":core:repository")      // Repository implementations
//include(":core:datasource")      // Local and remote data sources

// Feature-specific Core
//include(":core:location")        // Location services wrapper
//include(":core:prayer")          // Prayer calculations, business logic
//include(":core:qibla")           // Qibla direction calculations

// UI Common
//include(":core:ui:components")   // Reusable composables/views
//include(":core:ui:theme")        // MaterialTheme, colors, typography
//include(":core:ui:navigation")   // Navigation setup, destinations

// Features
//include(":feature:home")
//include(":feature:prayertimes")
//include(":feature:qibla")
//include(":feature:settings")
include(":prayer:domain")
include(":prayer:model")
include(":prayer:data")
