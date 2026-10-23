package com.kutluoglu.prayer_feature.settings.background

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import com.kutluoglu.prayer_location.data.CropMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class BackgroundAdjustViewModel(
    private val locationId: String,
    private val imageUri: Uri?,
    private val locationsCoordinator: LocationsCoordinator,
    private val backgroundImageStore: BackgroundImageStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackgroundAdjustUiState(uri = imageUri?.toString()))
    val uiState: StateFlow<BackgroundAdjustUiState> = _uiState.asStateFlow()

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack: StateFlow<Boolean> = _navigateBack.asStateFlow()

    /** Frame size in pixels, captured from the UI via [FrameSizeChanged]. */
    internal var frameSize: Size = Size.Zero

    /** Displayed image size in pixels, captured from the UI via [ImageLoaded]. */
    internal var displayedImageSize: Size = Size.Zero

    fun onEvent(event: BackgroundAdjustEvent) {
        when (event) {
            is BackgroundAdjustEvent.OnTransform -> {
                val (scale, offset) = CropMath.clampTransform(
                    scale = event.scale,
                    offset = event.offset,
                    imageAspectRatio = currentImageAspectRatio(),
                    frameAspectRatio = currentFrameAspectRatio()
                )
                _uiState.value = _uiState.value.copy(scale = scale, offset = offset)
            }
            BackgroundAdjustEvent.OnDoubleTap -> {
                val coverScale = maxOf(
                    1f,
                    currentImageAspectRatio() / currentFrameAspectRatio().coerceAtLeast(0.001f)
                )
                _uiState.value = _uiState.value.copy(scale = coverScale, offset = Offset.Zero)
            }
            BackgroundAdjustEvent.Confirm -> confirm()
        }
    }

    private fun confirm() {
        val uri = _uiState.value.uri ?: run { _navigateBack.value = false; return }
        // The UI must have measured the frame and loaded the image before a
        // crop can be computed; otherwise treat as a failed save.
        if (frameSize.width <= 0f || frameSize.height <= 0f ||
            displayedImageSize.width <= 0f || displayedImageSize.height <= 0f
        ) {
            _navigateBack.value = false
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            val cropRect = CropMath.normalizedCropRect(
                scale = _uiState.value.scale,
                offset = _uiState.value.offset,
                frameSize = frameSize,
                displayedImageSize = displayedImageSize
            )
            val path = backgroundImageStore.saveBakedCrop(
                imageUri ?: return@launch, cropRect, locationId
            )
            if (path != null) {
                // Capture the previous path BEFORE persisting the new one, so
                // the orphan cleanup knows which file to consider for deletion.
                val previousPath = locationsCoordinator.observeState().first()
                    .entries.firstOrNull { it.id == locationId }?.backgroundPath
                locationsCoordinator.setBackgroundPath(locationId, path)
                _navigateBack.value = true
                // Orphan cleanup is fire-and-forget: navigation must not wait
                // on a disk scan, and the file is harmless if cleanup fails.
                // Runs entirely on IO so it never resumes on Main after the
                // caller's scope is gone.
                viewModelScope.launch(Dispatchers.IO) {
                    deleteOrphanedBackground(newPath = path, previousPath = previousPath)
                }
            } else {
                _navigateBack.value = false
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    /**
     * After a successful replace, deletes the location's previous background
     * file if no other location still references it. Files are kept when a
     * location is removed (handled elsewhere), so this only runs on replace.
     */
    private suspend fun deleteOrphanedBackground(newPath: String, previousPath: String?) {
        if (previousPath == null || previousPath == newPath) return
        val stillReferenced = locationsCoordinator.observeState().firstOrNull()
            ?.entries
            ?.any { it.backgroundPath == previousPath }
            ?: false
        if (!stillReferenced) {
            backgroundImageStore.deleteBackground(previousPath)
        }
    }

    private fun currentImageAspectRatio(): Float {
        val size = displayedImageSize
        if (size.width <= 0f || size.height <= 0f) return 1f
        return size.width / size.height
    }

    private fun currentFrameAspectRatio(): Float {
        val size = frameSize
        if (size.width <= 0f || size.height <= 0f) return 1f
        return size.width / size.height
    }
}
