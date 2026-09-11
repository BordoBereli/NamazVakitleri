package com.kutluoglu.prayer_feature.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.core.designsystem.utils.LanguageProvider
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer.model.quran.SavedVersesSortOrder
import com.kutluoglu.prayer.usecases.quran.GetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesSortOrderUseCase
import com.kutluoglu.prayer.usecases.quran.GetSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.ReorderSavedVersesUseCase
import com.kutluoglu.prayer.usecases.quran.SetCollapsedSurahsUseCase
import com.kutluoglu.prayer.usecases.quran.SetSavedVersesSortOrderUseCase
import com.kutluoglu.prayer.usecases.quran.ToggleSavedVerseUseCase
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
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
    private val getCollapsedSurahsUseCase: GetCollapsedSurahsUseCase,
    private val setCollapsedSurahsUseCase: SetCollapsedSurahsUseCase,
    private val getSavedVersesSortOrderUseCase: GetSavedVersesSortOrderUseCase,
    private val setSavedVersesSortOrderUseCase: SetSavedVersesSortOrderUseCase,
    private val verseFormatter: QuranVerseFormatter,
    private val context: Context,
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
            is SavedVersesEvent.OnReorderGroups -> reorderGroups(event.groups)
            is SavedVersesEvent.OnReorderWithinGroup -> reorderWithinGroup(event.surahNumber, event.verses)
            is SavedVersesEvent.OnToggleCollapse -> toggleCollapse(event.surahNumber)
            SavedVersesEvent.OnExpandAll -> expandAll()
            SavedVersesEvent.OnCollapseAll -> collapseAll()
            is SavedVersesEvent.OnChangeSortOrder -> changeSortOrder(event.order)
            is SavedVersesEvent.OnSearch -> search(event.query)
            is SavedVersesEvent.OnSelect -> selectVerse(event.verse)
            SavedVersesEvent.OnDismissDetail -> dismissDetail()
        }
    }

    fun reload() {
        loadSavedVerses()
    }

    private fun loadSavedVerses() {
        viewModelScope.launch {
            _uiState.value = SavedVersesUiState.Loading
            val language = languageProvider.getLanguageCode()
            val collapsed = getCollapsedSurahsUseCase()
            val sortOrder = getSavedVersesSortOrderUseCase()
            getSavedVersesUseCase(language)
                .onSuccess { groups ->
                    _uiState.value = SavedVersesUiState.Success(
                        groups = groups,
                        filteredGroups = sortGroups(filterGroups(groups, ""), sortOrder),
                        collapsedSurahs = collapsed,
                        sortOrder = sortOrder
                    )
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

    private fun reorderGroups(groups: List<SavedVerseGroup>) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val currentSurahs = current.groups.map { it.surah.number }.toSet()
            val incomingSurahs = groups.map { it.surah.number }.toSet()
            if (incomingSurahs != currentSurahs) {
                Log.e("SavedVersesViewModel", "Ignoring group reorder while a search filter is active")
                loadSavedVerses()
                return@launch
            }
            reorderSavedVersesUseCase(groups)
                .onSuccess { updateGroups(groups) }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder groups -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun reorderWithinGroup(surahNumber: Int, verses: List<AyahData>) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val currentGroup = current.groups.firstOrNull { it.surah.number == surahNumber } ?: return@launch
            val currentNumbers = currentGroup.verses.map { it.numberInSurah }.toSet()
            val incomingNumbers = verses.map { it.numberInSurah }.toSet()
            if (incomingNumbers != currentNumbers) {
                Log.e("SavedVersesViewModel", "Ignoring within-group reorder while a search filter is active")
                loadSavedVerses()
                return@launch
            }
            val groups = current.groups.map { group ->
                if (group.surah.number == surahNumber) group.copy(verses = verses) else group
            }
            reorderSavedVersesUseCase(groups)
                .onSuccess { updateGroups(groups) }
                .onFailure {
                    Log.e("SavedVersesViewModel", "Failed to reorder verses -> ${it.message}")
                    loadSavedVerses()
                }
        }
    }

    private fun toggleCollapse(surahNumber: Int) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val collapsed = if (surahNumber in current.collapsedSurahs) {
                current.collapsedSurahs - surahNumber
            } else {
                current.collapsedSurahs + surahNumber
            }
            _uiState.value = current.copy(collapsedSurahs = collapsed)
            runCatching { setCollapsedSurahsUseCase(collapsed) }
                .onFailure { Log.e("SavedVersesViewModel", "Failed to persist collapse state -> ${it.message}") }
        }
    }

    private fun expandAll() {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            _uiState.value = current.copy(collapsedSurahs = emptySet())
            runCatching { setCollapsedSurahsUseCase(emptySet()) }
                .onFailure { Log.e("SavedVersesViewModel", "Failed to persist collapse state -> ${it.message}") }
        }
    }

    private fun collapseAll() {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            val collapsed = current.groups.map { it.surah.number }.toSet()
            _uiState.value = current.copy(collapsedSurahs = collapsed)
            runCatching { setCollapsedSurahsUseCase(collapsed) }
                .onFailure { Log.e("SavedVersesViewModel", "Failed to persist collapse state -> ${it.message}") }
        }
    }

    private fun search(query: String) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(
            query = query,
            filteredGroups = sortGroups(filterGroups(current.groups, query), current.sortOrder)
        )
    }

    private fun updateGroups(groups: List<SavedVerseGroup>) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(
            groups = groups,
            filteredGroups = sortGroups(filterGroups(groups, current.query), current.sortOrder)
        )
    }

    private fun changeSortOrder(order: SavedVersesSortOrder) {
        viewModelScope.launch {
            val current = _uiState.value as? SavedVersesUiState.Success ?: return@launch
            _uiState.value = current.copy(
                sortOrder = order,
                filteredGroups = sortGroups(filterGroups(current.groups, current.query), order)
            )
            runCatching { setSavedVersesSortOrderUseCase(order) }
                .onFailure { Log.e("SavedVersesViewModel", "Failed to persist sort order -> ${it.message}") }
        }
    }

    private fun sortGroups(
        groups: List<SavedVerseGroup>,
        order: SavedVersesSortOrder
    ): List<SavedVerseGroup> = when (order) {
        SavedVersesSortOrder.MANUAL -> groups
        SavedVersesSortOrder.SURAH_NUMBER -> groups.sortedBy { it.surah.number }
        SavedVersesSortOrder.SURAH_NAME ->
            groups.sortedBy { verseFormatter.getLocalizedNameOf(it.surah, context).lowercase() }
        SavedVersesSortOrder.VERSE_COUNT -> groups.sortedByDescending { it.verses.size }
        SavedVersesSortOrder.DATE_SAVED ->
            groups.sortedByDescending { it.verses.maxOfOrNull { v -> v.savedAt ?: 0L } ?: 0L }
    }

    private fun selectVerse(verse: AyahData) {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = verse, isDetailVisible = true)
    }

    private fun dismissDetail() {
        val current = _uiState.value as? SavedVersesUiState.Success ?: return
        _uiState.value = current.copy(selectedVerse = null, isDetailVisible = false)
    }

    private fun filterGroups(groups: List<SavedVerseGroup>, query: String): List<SavedVerseGroup> {
        if (query.isBlank()) return groups
        val q = query.trim().lowercase()
        return groups.mapNotNull { group ->
            val surahMatches = group.surah.englishName.lowercase().contains(q) ||
                group.surah.name.lowercase().contains(q)
            val matchingVerses = group.verses.filter { it.text.lowercase().contains(q) }
            when {
                surahMatches && matchingVerses.isNotEmpty() -> group.copy(verses = matchingVerses)
                surahMatches -> group
                matchingVerses.isNotEmpty() -> group.copy(verses = matchingVerses)
                else -> null
            }
        }
    }
}
