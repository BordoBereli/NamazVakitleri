package com.kutluoglu.prayer_feature.settings.background

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.kutluoglu.core.designsystem.components.BackNavigationIcon
import com.kutluoglu.core.designsystem.components.RoundedPageTitleBar
import com.kutluoglu.prayer_feature.settings.R
import java.io.File
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundLocationsRoute(
    onNavigateBack: () -> Unit,
    onPickImage: (locationId: String, imageUri: String) -> Unit,
    viewModel: BackgroundLocationsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var deleteCandidate by remember { mutableStateOf<String?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var pendingPickLocationId by remember { mutableStateOf<String?>(null) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }

    // Re-read the gallery whenever the screen comes back to the foreground,
    // so a background saved on the adjust screen shows up immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val locationId = pendingPickLocationId
        pendingPickLocationId = null
        if (uri != null && locationId != null) {
            onPickImage(locationId, Uri.encode(uri.toString()))
        }
    }

    Scaffold(
        topBar = {
            RoundedPageTitleBar(
                title = stringResource(R.string.background_locations),
                navigationIcon = { BackNavigationIcon(onClick = onNavigateBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.background_image),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (state.locations.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.background_no_locations),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            items(state.locations, key = { it.id }) { location ->
                BackgroundLocationRow(
                    location = location,
                    isSelected = location.id == selectedLocationId,
                    onSelect = { selectedLocationId = location.id },
                    onPickImage = {
                        pendingPickLocationId = location.id
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onReset = { viewModel.onEvent(BackgroundLocationsEvent.ResetBackground(location.id)) }
                )
            }
            if (state.savedBackgrounds.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.saved_backgrounds),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                        )
                        TextButton(onClick = { showDeleteAllDialog = true }) {
                            Text(
                                text = stringResource(R.string.delete_all_backgrounds),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.savedBackgrounds, key = { it }) { path ->
                            SavedBackgroundThumbnail(
                                path = path,
                                onApply = {
                                    val locationId = selectedLocationId
                                    if (locationId != null) {
                                        viewModel.onEvent(
                                            BackgroundLocationsEvent.ApplySavedBackground(locationId, path)
                                        )
                                    }
                                },
                                onDelete = { deleteCandidate = path }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteCandidate?.let { path ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_background)) },
            text = { Text(stringResource(R.string.delete_background_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(BackgroundLocationsEvent.DeleteBackground(path))
                    deleteCandidate = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.delete_all_backgrounds)) },
            text = { Text(stringResource(R.string.delete_all_backgrounds_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(BackgroundLocationsEvent.DeleteAllBackgrounds)
                    showDeleteAllDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun BackgroundLocationRow(
    location: BackgroundLocationUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPickImage: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackgroundThumbnail(path = location.backgroundPath)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(location.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.background_image),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPickImage) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = stringResource(R.string.background_image),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (location.backgroundPath != null) {
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.reset_background),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedBackgroundThumbnail(
    path: String,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onApply)
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = stringResource(R.string.apply_background),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_background),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun BackgroundThumbnail(path: String?) {
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
