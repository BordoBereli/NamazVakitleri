package com.kutluoglu.namazvakitleri

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kutluoglu.core.designsystem.theme.NamazVakitleriTheme
import com.kutluoglu.prayer_settings.domain.repository.SettingsRepository
import org.koin.compose.koinInject

class MainActivity : AppCompatActivity() {

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
