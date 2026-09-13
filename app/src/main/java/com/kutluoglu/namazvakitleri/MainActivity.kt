package com.kutluoglu.namazvakitleri

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kutluoglu.core.designsystem.theme.NamazVakitleriTheme
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.android.ext.android.get
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localeManager = get<LocaleManager>()
        val settingsDataStore = get<SettingsDataStore>()
        super.attachBaseContext(localeManager.applyPersistedLocale(newBase, settingsDataStore))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository: SettingsRepository = koinInject()
            val settings by settingsRepository.observeSettings().collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                "light" -> false
                "dark" -> true
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            NamazVakitleriTheme(darkTheme = darkTheme) {
                MainAppScreen()
            }
        }
    }
}
