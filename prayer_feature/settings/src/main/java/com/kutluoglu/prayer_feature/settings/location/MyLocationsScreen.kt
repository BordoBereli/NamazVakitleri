package com.kutluoglu.prayer_feature.settings.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kutluoglu.prayer.model.location.LocationEntry
import com.kutluoglu.prayer_feature.settings.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLocationsRoute(
    onNavigateBack: () -> Unit,
    onAddLocation: () -> Unit,
    viewModel: MyLocationsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val entries = remember { mutableStateListOf<LocationEntry>() }
    var pendingOrder by remember { mutableStateOf<List<String>?>(null) }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = entries.indexOfFirst { it.id == from.key }
        val toIndex = entries.indexOfFirst { it.id == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            entries.add(toIndex, entries.removeAt(fromIndex))
        }
    }

    val context = LocalContext.current
    val activity = LocalActivity.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var permissionCheckTrigger by remember { mutableIntStateOf(0) }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionGranted = remember(permissionCheckTrigger) { hasLocationPermission() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(true))
        } else {
            val permanentDenial = activity == null ||
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            scope.launch {
                val actionLabel = if (permanentDenial) context.getString(R.string.open_settings) else null
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.location_permission_required),
                    actionLabel = actionLabel,
                    duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionCheckTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.entries) {
        val currentOrder = state.entries.filterNot { it.isAutoGps }.map { it.id }
        val pending = pendingOrder
        if (pending != null) {
            if (currentOrder == pending || currentOrder.toSet() != pending.toSet()) {
                pendingOrder = null
            } else {
                return@LaunchedEffect
            }
        }
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        entries.clear()
        entries.addAll(state.entries)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val persisted = entries.filterNot { it.isAutoGps }
                val current = state.entries.filterNot { it.isAutoGps }
                if (persisted.isNotEmpty() && persisted.map { it.id } != current.map { it.id }) {
                    pendingOrder = persisted.map { it.id }
                    viewModel.onEvent(MyLocationsEvent.ReorderLocations(persisted.map { it.id }))
                }
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_locations)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.use_my_current_location), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.auto_updates_as_you_travel), style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.gpsEnabled && permissionGranted,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (permissionGranted) {
                                viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(true))
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        } else {
                            viewModel.onEvent(MyLocationsEvent.SetGpsEnabled(false))
                        }
                    }
                )
            }
            Button(
                onClick = onAddLocation,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_location))
            }
            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_locations_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(state = lazyListState) {
                    items(entries, key = { it.id }) { entry ->
                        ReorderableItem(
                            state = reorderableState,
                            key = entry.id,
                            enabled = !entry.isAutoGps
                        ) { isDragging ->
                            LocationRow(
                                entry = entry,
                                isSelected = entry.id == state.selectedId,
                                isDragging = isDragging,
                                onSelect = { viewModel.onEvent(MyLocationsEvent.SelectLocation(entry.id)) },
                                onDelete = {
                                    if (!entry.isAutoGps) {
                                        viewModel.onEvent(MyLocationsEvent.RemoveLocation(entry.id))
                                    }
                                },
                                dragHandle = if (entry.isAutoGps) null else {
                                    {
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(
    entry: LocationEntry,
    isSelected: Boolean,
    isDragging: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (entry.isAutoGps) stringResource(R.string.gps_badge) else stringResource(R.string.manual_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.isAutoGps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
            }
            if (!entry.isAutoGps) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            dragHandle?.invoke()
        }
    }
}
