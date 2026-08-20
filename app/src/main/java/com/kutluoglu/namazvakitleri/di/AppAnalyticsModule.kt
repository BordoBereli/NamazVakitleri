package com.kutluoglu.namazvakitleri.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.kutluoglu.core.common.analytics.AnalyticsTracker
import com.kutluoglu.namazvakitleri.analytics.AnalyticsUserPropertiesManager
import com.kutluoglu.namazvakitleri.analytics.FirebaseAnalyticsTracker
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Koin module for the app's analytics wiring.
 *
 * The `:app` module has no `@ComponentScan` root, so standalone `@Single` classes in
 * the app package would land in the (unloaded) default module. Registering them here
 * (the same pattern used by library modules) guarantees they are in the Koin graph.
 */
@Module
@Configuration
object AppAnalyticsModule {

    @Single
    fun provideAnalyticsTracker(context: Context): AnalyticsTracker =
        FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context))

    @Single
    fun provideUserPropertiesManager(
        analyticsTracker: AnalyticsTracker,
        settingsRepository: SettingsRepository,
        locationsCoordinator: LocationsCoordinator
    ): AnalyticsUserPropertiesManager =
        AnalyticsUserPropertiesManager(analyticsTracker, settingsRepository, locationsCoordinator)
}
