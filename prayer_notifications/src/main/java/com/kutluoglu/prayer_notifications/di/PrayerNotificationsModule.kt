package com.kutluoglu.prayer_notifications.di

import android.content.Context
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_notifications**")
object PrayerNotificationsModule {

    @Single
    fun provideNotificationSettingsDataStore(context: Context): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context)
}
