package com.kutluoglu.namazvakitleri

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kutluoglu.core.designsystem.theme.NamazVakitleriTheme
import com.kutluoglu.namazvakitleri.locale.LocaleManager
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localeManager = get<LocaleManager>()
        val settingsDataStore = get<SettingsDataStore>()
        val language = runCatching { runBlocking { settingsDataStore.getSettings().language } }
            .getOrDefault(LocaleManager.SYSTEM_LANGUAGE)
        localeManager.setLanguage(language)
        super.attachBaseContext(localeManager.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamazVakitleriTheme(darkTheme = true) {
               MainAppScreen()
            }
        }
    }
}
