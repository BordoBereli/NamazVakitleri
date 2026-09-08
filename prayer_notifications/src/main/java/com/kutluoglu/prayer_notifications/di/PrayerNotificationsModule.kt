package com.kutluoglu.prayer_notifications.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.firebase.messaging.FirebaseMessaging
import com.kutluoglu.prayer_notifications.data.NotificationSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("com.kutluoglu.prayer_notifications**")
object PrayerNotificationsModule {

    @Single
    fun provideNotificationSettingsDataStore(context: Context): NotificationSettingsDataStore =
        NotificationSettingsDataStore.create(context)

    @Single
    fun providePlayer(context: Context): Player =
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            .build()

    @Single
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Single
    @Named("pushScope")
    fun providePushScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}