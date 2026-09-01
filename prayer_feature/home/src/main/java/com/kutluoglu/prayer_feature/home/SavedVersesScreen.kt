package com.kutluoglu.prayer_feature.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kutluoglu.core.designsystem.components.EmptyStateContent
import com.kutluoglu.core.designsystem.components.LoadingIndicator
import com.kutluoglu.prayer.model.quran.AyahData
import com.kutluoglu.prayer.model.quran.SavedVerseGroup
import com.kutluoglu.prayer_feature.home.common.QuranVerseFormatter
import com.kutluoglu.prayer_feature.home.common.shareVerse
import com.kutluoglu.prayer_feature.home.feature.CustomBottomSheet
import com.kutluoglu.prayer_feature.home.feature.VerseDetailSheetContent
import com.kutluoglu.prayer_feature.home.state.SavedVersesUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface SavedRow {
    val key: String
    data class Header(val group: SavedVerseGroup) : SavedRow {
        override val key: String = "h-${group.surah.number}"
    }
    data class Verse(val group: SavedVerseGroup, val verse: AyahData) : SavedRow {
        override val key: String = "v-${group.surah.number}-${verse.numberInSurah}"
    }
}

private fun List<SavedVerseGroup>.toRows(): List<SavedRow> = flatMap { group ->
    listOf(SavedRow.Header(group)) + group.verses.map { SavedRow.Verse(group, it) }
}

private fun rowsToGroups(rows: List<SavedRow>): List<SavedVerseGroup> =
    rows.filterIsInstance<SavedRow.Header>().map { it.group }

private fun moveGroup(groups: List<SavedVerseGroup>, from: Int, to: Int): List<SavedVerseGroup> {
    val mutable = groups.toMutableList()
    val group = mutable.removeAt(from)
    mutable.add(to.coerceIn(0, mutable.size), group)
    return mutable
}

private fun AyahData.samePosition(other: AyahData): Boolean =
    surah.number == other.surah.number && numberInSurah == other.numberInSurah

@Composable
fun SavedVersesRoute(
    onNavigateBack: () -> Unit,
    viewModel: SavedVersesViewModel = koinViewModel(),
    verseFormatter: QuranVerseFormatter = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val rows = remember { mutableStateListOf<SavedRow>() }
    var pendingReorder by remember { mutableStateOf<SavedVersesEvent?>(null) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val success = state as? SavedVersesUiState.Success ?: return@rememberReorderableLazyListState
        if (success.query.isNotBlank()) return@rememberReorderableLazyListState
        val fromIndex = rows.indexOfFirst { it.key == from.key }
        val toIndex = rows.indexOfFirst { it.key == to.key }
        if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
        val fromRow = rows[fromIndex]
        when (fromRow) {
            is SavedRow.Header -> {
                val fromGroupIndex = rows.take(fromIndex).count { it is SavedRow.Header }
                val toGroupIndex = rows.take(toIndex).count { it is SavedRow.Header }
                val newGroups = moveGroup(rowsToGroups(rows), fromGroupIndex, toGroupIndex)
                rows.clear()
                rows.addAll(newGroups.toRows())
                pendingReorder = SavedVersesEvent.OnReorderGroups(newGroups)
            }
            is SavedRow.Verse -> {
                val toRow = rows[toIndex]
                if (toRow !is SavedRow.Verse || toRow.group.surah.number != fromRow.group.surah.number) {
                    return@rememberReorderableLazyListState
                }
                val group = rowsToGroups(rows).first { it.surah.number == fromRow.group.surah.number }
                val fromVerseIndex = group.verses.indexOfFirst { it.samePosition(fromRow.verse) }
                val toVerseIndex = group.verses.indexOfFirst { it.samePosition(toRow.verse) }
                if (fromVerseIndex == -1 || toVerseIndex == -1) return@rememberReorderableLazyListState
                val newVerses = group.verses.toMutableList().apply {
                    add(toVerseIndex, removeAt(fromVerseIndex))
                }
                val newGroups = rowsToGroups(rows).map { g ->
                    if (g.surah.number == group.surah.number) g.copy(verses = newVerses) else g
                }
                rows.clear()
                rows.addAll(newGroups.toRows())
                pendingReorder = SavedVersesEvent.OnReorderWithinGroup(group.surah.number, newVerses)
            }
        }
    }

    LaunchedEffect(state) {
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        val success = state as? SavedVersesUiState.Success ?: return@LaunchedEffect
        val searching = success.query.isNotBlank()
        val target = success.filteredGroups.toRows().filterNot { row ->
            !searching && row is SavedRow.Verse && row.group.surah.number in success.collapsedSurahs
        }
        if (rows.toList() != target) {
            rows.clear()
            rows.addAll(target)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .distinctUntilChanged()
            .drop(1)
            .filter { !it }
            .collect {
                pendingReorder?.let { onEvent(it) }
                pendingReorder = null
            }
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
                if (state.groups.isEmpty()) {
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
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        SearchField(
                            query = state.query,
                            onQueryChange = { onEvent(SavedVersesEvent.OnSearch(it)) }
                        )
                        if (state.query.isBlank()) {
                            SurahJumpChips(
                                groups = state.filteredGroups,
                                verseFormatter = verseFormatter,
                                context = context,
                                onJump = { surahNumber ->
                                    val index = rows.indexOfFirst {
                                        it is SavedRow.Header && it.group.surah.number == surahNumber
                                    }
                                    if (index != -1) scope.launch { lazyListState.scrollToItem(index) }
                                }
                            )
                        }
                        if (state.filteredGroups.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateContent(
                                    icon = Icons.Default.Search,
                                    text = stringResource(R.string.no_matching_verses)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                rows.forEach { row ->
                                    when (row) {
                                        is SavedRow.Header -> item(key = row.key) {
                                            ReorderableItem(state = reorderableState, key = row.key) { isDragging ->
                                                SurahHeader(
                                                    group = row.group,
                                                    isCollapsed = state.query.isBlank() &&
                                                        row.group.surah.number in state.collapsedSurahs,
                                                    verseFormatter = verseFormatter,
                                                    context = context,
                                                    onToggle = {
                                                        onEvent(SavedVersesEvent.OnToggleCollapse(row.group.surah.number))
                                                    },
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
                                        is SavedRow.Verse -> item(key = row.key) {
                                            ReorderableItem(state = reorderableState, key = row.key) { isDragging ->
                                                val dismissState = rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { value ->
                                                        if (value != SwipeToDismissBoxValue.Settled) {
                                                            rows.remove(row)
                                                            onEvent(SavedVersesEvent.OnRemove(row.verse))
                                                        }
                                                        true
                                                    }
                                                )
                                                SwipeToDismissBox(
                                                    state = dismissState,
                                                    enableDismissFromStartToEnd = true,
                                                    enableDismissFromEndToStart = false,
                                                    backgroundContent = {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 16.dp, vertical = 4.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(44.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(
                                                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Delete,
                                                                    contentDescription = stringResource(R.string.delete),
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    VerseRow(
                                                        verse = row.verse,
                                                        isDragging = isDragging,
                                                        onSelect = { onEvent(SavedVersesEvent.OnSelect(row.verse)) },
                                                        onShare = { shareVerse(row.verse, verseFormatter, context) },
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search_saved_verses)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            }
        } else {
            null
        },
        singleLine = true
    )
}

@Composable
private fun SurahJumpChips(
    groups: List<SavedVerseGroup>,
    verseFormatter: QuranVerseFormatter,
    context: Context,
    onJump: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groups, key = { it.surah.number }) { group ->
            FilterChip(
                selected = false,
                onClick = { onJump(group.surah.number) },
                label = { Text(verseFormatter.getLocalizedNameOf(group.surah, context)) }
            )
        }
    }
}

@Composable
private fun SurahHeader(
    group: SavedVerseGroup,
    isCollapsed: Boolean,
    verseFormatter: QuranVerseFormatter,
    context: Context,
    onToggle: () -> Unit,
    dragHandle: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("surah_index_${group.surah.number}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${group.surah.number}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verseFormatter.getLocalizedNameOf(group.surah, context),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${group.verses.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            dragHandle()
        }
    }
}

@Composable
private fun VerseRow(
    verse: AyahData,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onShare: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("ayah_index_${verse.surah.number}_${verse.numberInSurah}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${verse.numberInSurah}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_verse)
                        )
                    }
                    dragHandle?.invoke()
                }
                Text(
                    text = verse.text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}
