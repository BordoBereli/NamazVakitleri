package com.kutluoglu.prayer_feature.home.domain

import android.util.Log
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.usecases.quran.GetRandomVerseUseCase
import com.kutluoglu.prayer.usecases.quran.IsVerseSavedUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.QuranUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class QuranVerseLoader(
    private val getRandomVerseUseCase: GetRandomVerseUseCase,
    private val isVerseSavedUseCase: IsVerseSavedUseCase,
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase,
    private val languageProvider: LanguageProvider
) {
    private val _quranState = MutableStateFlow(QuranUiState())
    val quranState: StateFlow<QuranUiState> = _quranState

    /**
     * Polls until [isScreenReady] returns true (1s backoff doubling up to 30s),
     * then fetches the verse exactly once and reflects its saved state.
     */
    fun loadVerse(scope: CoroutineScope, isScreenReady: () -> Boolean) {
        scope.launch {
            var delayMillis = 1_000L
            while (true) {
                if (isScreenReady()) {
                    val language = languageProvider.getLanguageCode()
                    getRandomVerseUseCase(language)
                        .onSuccess { verse ->
                            val isSaved = isVerseSavedUseCase(verse)
                            _quranState.value = _quranState.value.copy(verse = verse, isSaved = isSaved)
                        }
                        .onFailure {
                            Log.e("QuranVerseLoader", "Failed to load random verse -> ${it.message}")
                        }
                    break
                }
                delay(delayMillis)
                delayMillis = (delayMillis * 2).coerceAtMost(30_000L)
            }
        }
    }

    fun toggleSaved(verse: AyahData, scope: CoroutineScope) {
        scope.launch {
            toggleSavedVerseUseCase(verse)
                .onSuccess {
                    _quranState.value = _quranState.value.copy(isSaved = !_quranState.value.isSaved)
                }
                .onFailure {
                    Log.e("QuranVerseLoader", "Failed to toggle saved verse -> ${it.message}")
                }
        }
    }

    fun setSheetVisible(isVisible: Boolean) {
        _quranState.value = _quranState.value.copy(isSheetVisible = isVisible)
    }
}
