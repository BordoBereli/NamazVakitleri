package com.kutluoglu.prayer_auto.service

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator
import com.kutluoglu.prayer.domain.PrayerSurfaceDataProvider
import com.kutluoglu.prayer_auto.session.PrayerCarSession
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Entry point for Android Auto. Instantiated by the Android system (not Koin),
 * so it resolves [PrayerSurfaceDataProvider] via [KoinComponent].
 */
class PrayerCarAppService : CarAppService(), KoinComponent {

    private val surfaceProvider: PrayerSurfaceDataProvider by inject()

    override fun onCreateSession(sessionInfo: SessionInfo): Session =
        PrayerCarSession(surfaceProvider)

    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }
}
