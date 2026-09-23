package com.kutluoglu.namazvakitleri

import android.content.Context
import com.kutluoglu.core.common.AppVersion
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.namazvakitleri.notifications.NotificationRescheduler
import com.kutluoglu.namazvakitleri.notifications.PrayerCalculationSettingsProviderImpl
import com.kutluoglu.namazvakitleri.push.PushTopicCoordinator
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_notifications.domain.PrayerCalculationSettingsProvider
import com.kutluoglu.prayer_notifications.push.TopicSubscriptionManager
import com.kutluoglu.prayer_notifications.scheduler.AlarmScheduler
import com.kutluoglu.prayer.settings.SettingsProvider
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import com.kutluoglu.prayer_settings.domain.usecase.GetSettingsUseCase
import com.kutluoglu.prayer_widget.WidgetRefresher
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Koin module for the app's settings/data wiring.
 *
 * The `:app` module has no `@ComponentScan` root, so standalone `@Single` classes in
 * the app package would land in the (unloaded) default module. Registering them here
 * (the same pattern used by library modules) guarantees they are in the Koin graph.
 *
 * Settings use cases, repositories and ViewModels are registered by their own KSP
 * annotations via `@ComponentScan` in `PrayerSettingsModule` / `PrayerFeatureSettingsModule`.
 */
@Module
@Configuration
object AppModule {

    @Single
    fun provideLocaleManager(): LocaleManager = LocaleManager()

    @Single
    fun provideAppVersion(): AppVersion = AppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    @Single
    fun provideNotificationRescheduler(
        scheduler: AlarmScheduler,
        settingsRepository: SettingsRepository,
        locationsCoordinator: LocationsCoordinator
    ): NotificationRescheduler = NotificationRescheduler(scheduler, settingsRepository, locationsCoordinator)

    @Single
    fun providePrayerCalculationSettingsProvider(
        getSettingsUseCase: GetSettingsUseCase
    ): PrayerCalculationSettingsProvider = PrayerCalculationSettingsProviderImpl(getSettingsUseCase)

    @Single
    fun providePushTopicCoordinator(
        topicSubscriptionManager: TopicSubscriptionManager,
        locationsCoordinator: LocationsCoordinator
    ): PushTopicCoordinator = PushTopicCoordinator(topicSubscriptionManager, locationsCoordinator)

    @Single
    fun provideWidgetRefresher(
        settingsProvider: SettingsProvider,
        locationsCoordinator: LocationsCoordinator,
        context: Context
    ): WidgetRefresher = WidgetRefresher.create(settingsProvider, locationsCoordinator, context)

    @Single
    fun provideSettingsDataStore(context: Context): SettingsDataStore = SettingsDataStore.create(context)
}
