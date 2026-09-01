package com.kutluoglu.prayer_feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SavedVersesViewModel(
    private val getSavedVersesUseCase: GetSavedVersesUseCase,
    private val reorderSavedVersesUseCase: ReorderSavedVersesUseCase,
    private val toggleSavedVerseUseCase: ToggleSavedVerseUseCase,
    private val languageProvider: LanguageProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedVersesUiState>(SavedVersesUiState.Loading)
    val uiState: StateFlow<SavedVersesUiState> = _uiState.asStateFlow()

    init {
        loadSavedVerses()
    }

    fun onEvent(event: SavedVersesEvent) {
        when (event) {
            is SavedVersesEvent.OnRemove -> removeVerse(event.verse)
            is SavedVersesEvent.OnReorder -> reorder(event.verses)
            is SavedVersesEvent.OnSelect -> selectVerse(event.verse)
            SavedVersesEvent.OnDismissDetail -> dismissDetail()
        }
    }

    /**
     * Re-fetches the saved verses. Called when the screen becomes visible so the
     * list reflects verses saved since the ViewModel was created (a one-shot load
     * in [init] would otherwise show stale data if the instance is reused).
     */
    fun reload() {
        loadSavedVerses()
    }

    private fun loadSavedVerses() {
        viewModelScope.launch {
            _uiState.value = SavedVersesUiState.Loading
            val language = languageProvider.getLanguageCode()
            getSavedVersesUseCase(language)
                .onSuccess { verses ->
                    Log.d("SavedVersesViewModel", "Loaded ${verses.size} saved verses")
                    _uiState.value = SavedVersesUiState.Success(verses)
                }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to load saved verses -> ${it.message}")
                    _uiState.value = SavedVersesUiState.Error(
                        it.message ?: "Saved verses could not be loaded."
                    )
                }
        }
    }

    private fun removeVerse(verse: AyahData) {
        viewModelScope.launch {
            toggleSavedVerseUseCase(verse)
                .onSuccess {
                    dismissDetail()
                    loadSavedVerses()
                }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to remove saved verse -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun reorder(verses: List<AyahData>) {
        viewModelScope.launch {
            reorderSavedVersesUseCase(verses)
                .onSuccess {
                    val current = _uiState.value as? SavedVersesUiState.Success ?: return@onSuccess
                    _uiState.value = current.copy(verses = verses)
                }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder saved verses -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun selectVerse(verse: AyahData) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = verse, isDetailVisible = true)
    }

    private fun dismissDetail() {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = null, isDetailVisible = false)
    }
}
