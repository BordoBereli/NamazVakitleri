package com.kutluoglu.namazvakitleri.locale

import android.content.Context
import com.kutluoglu.prayer_settings.data.local.SettingsDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-time migration of the persisted DataStore language preference into the
 * per-app language API (AppCompat storage).
 *
 * Flag-gated via SharedPreferences so it never overrides a language the user
 * later changed through system settings. Runs async from Application.onCreate.
 */
class LocaleMigration(
    private val context: Context,
    private val controller: AppLocaleController,
    private val settingsDataStore: SettingsDataStore
) {

    @Volatile
    private var migrated = false

    suspend fun migrateIfNeeded() {
        if (migrated) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            migrated = true
            return
        }

        try {
            val language = settingsDataStore.getSettings().language
            if (language != AppLocaleController.SYSTEM_LANGUAGE &&
                controller.getApplicationLocales() != language
            ) {
                withContext(Dispatchers.Main) {
                    controller.setApplicationLocales(language)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Migration must never crash the app; the flag is still set so we
            // don't retry a permanently failing read.
        } finally {
            migrated = true
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "locale_migration"
        private const val KEY_MIGRATED = "locale_migrated"
    }
}
