package com.kutluoglu.prayer_feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.EmptyStateContent
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.common.shareVerse
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SavedVersesRoute(
    onNavigateBack: () -> Unit,
    viewModel: SavedVersesViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.reload()
    }
    SavedVersesScreen(
        state = state,
        verseFormatter = verseFormatter,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedVersesScreen(
    state: SavedVersesUiState,
    verseFormatter: QuranVerseFormatter,
    onNavigateBack: () -> Unit,
    onEvent: (SavedVersesEvent) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val entries = remember { mutableStateListOf<AyahData>() }
    val context = LocalContext.current
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = entries.indexOfFirst { it.toString() == from.key }
        val toIndex = entries.indexOfFirst { it.toString() == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            entries.add(toIndex, entries.removeAt(fromIndex))
        }
    }

    LaunchedEffect(state) {
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        val success = state as? SavedVersesUiState.Success ?: return@LaunchedEffect
        if (entries.toList() != success.verses) {
            entries.clear()
            entries.addAll(success.verses)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .distinctUntilChanged()
            .drop(1)
            .filter { !it }
            .collect { onEvent(SavedVersesEvent.OnReorder(entries.toList())) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_verses)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            SavedVersesUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { LoadingIndicator() }

            is SavedVersesUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.message) }

            is SavedVersesUiState.Success -> {
                if (state.verses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateContent(
                            icon = Icons.Outlined.BookmarkBorder,
                            text = stringResource(R.string.no_saved_verses)
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        items(entries, key = { it.toString() }) { verse ->
                            ReorderableItem(state = reorderableState, key = verse.toString()) { isDragging ->
                                SwipeToDismissBox(
                                    state = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value != SwipeToDismissBoxValue.Settled) {
                                                entries.remove(verse)
                                                onEvent(SavedVersesEvent.OnRemove(verse))
                                            }
                                            true
                                        }
                                    ),
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                ) {
                                    VerseRow(
                                        verse = verse,
                                        verseFormatter = verseFormatter,
                                        isDragging = isDragging,
                                        onSelect = { onEvent(SavedVersesEvent.OnSelect(verse)) },
                                        onShare = { shareVerse(verse, verseFormatter, context) },
                                        dragHandle = {
                                            IconButton(
                                                modifier = Modifier.draggableHandle(),
                                                onClick = {}
                                            ) {
                                                Icon(
                                                    Icons.Rounded.DragHandle,
                                                    contentDescription = stringResource(R.string.reorder)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val success = state as? SavedVersesUiState.Success
    CustomBottomSheet(
        isVisible = success?.isDetailVisible == true,
        onDismiss = { onEvent(SavedVersesEvent.OnDismissDetail) }
    ) {
        success?.selectedVerse?.let { verse ->
            VerseDetailSheetContent(
                verse = verse,
                verseFormatter = verseFormatter,
                isSaved = true,
                onToggleSaved = { onEvent(SavedVersesEvent.OnRemove(verse)) }
            )
        }
    }
}

@Composable
private fun VerseRow(
    verse: AyahData,
    verseFormatter: QuranVerseFormatter,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val localizedSurahName = verseFormatter.getLocalizedNameOf(verse, context)
    val verseInfo = "($localizedSurahName - $verse)"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = verseInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share_verse)
                )
            }
            dragHandle?.invoke()
        }
    }
}
