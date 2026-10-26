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

    init {
        // Decode the preview bitmap up front: the screen draws it directly
        // (Canvas + drawImage), so the drawn geometry matches the crop math.
        imageUri?.let { uri ->
            viewModelScope.launch { loadPreview(uri) }
        }
    }

    /**
     * Loads the preview bitmap and publishes its size into the state.
     * Internal + suspend so tests can drive it deterministically.
     */
    internal suspend fun loadPreview(uri: Uri) {
        val bitmap = backgroundImageStore.loadPreviewBitmap(uri) ?: return
        _uiState.value = _uiState.value.copy(previewBitmap = bitmap)
        if (bitmap.width > 0 && bitmap.height > 0) {
            displayedImageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
            _uiState.value = _uiState.value.copy(
                imageAspectRatio = bitmap.width.toFloat() / bitmap.height
            )
            applyTransform(_uiState.value.scale, _uiState.value.offset)
        }
    }

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack: StateFlow<Boolean> = _navigateBack.asStateFlow()

    /** Frame size in pixels, captured from the UI via [BackgroundAdjustEvent.FrameMeasured]. */
    internal var frameSize: Size = Size.Zero

    /** Displayed image size in pixels, captured from the decoded preview bitmap. */
    internal var displayedImageSize: Size = Size.Zero

    fun onEvent(event: BackgroundAdjustEvent) {
        when (event) {
            is BackgroundAdjustEvent.OnTransform ->
                applyTransform(scale = event.scale, offset = event.offset)
            BackgroundAdjustEvent.OnDoubleTap -> applyTransform(
                scale = maxOf(
                    1f,
                    currentImageAspectRatio() / currentFrameAspectRatio().coerceAtLeast(0.001f)
                ),
                offset = Offset.Zero
            )
            is BackgroundAdjustEvent.FrameMeasured -> {
                frameSize = event.size
                applyTransform(_uiState.value.scale, _uiState.value.offset)
            }
            BackgroundAdjustEvent.Confirm -> confirm()
        }
    }

    /**
     * Stores [scale]/[offset] clamped so the frame always stays covered.
     *
     * The first render happens before the image is decoded, so raw values are
     * kept until both [frameSize] and [displayedImageSize] are known; the
     * [BackgroundAdjustEvent.FrameMeasured] event and the bitmap-load
     * completion re-run this clamp as soon as a measurement arrives.
     */
    private fun applyTransform(scale: Float, offset: Offset) {
        if (frameSize.width <= 0f || frameSize.height <= 0f ||
            displayedImageSize.width <= 0f || displayedImageSize.height <= 0f
        ) {
            _uiState.value = _uiState.value.copy(scale = scale, offset = offset)
            return
        }
        val (clampedScale, clampedOffset) = CropMath.clampTransform(
            scale = scale,
            offset = offset,
            imageAspectRatio = currentImageAspectRatio(),
            frameAspectRatio = currentFrameAspectRatio(),
            frameSize = frameSize
        )
        _uiState.value = _uiState.value.copy(scale = clampedScale, offset = clampedOffset)
    }

    private fun confirm() {
        if (_uiState.value.uri == null) {
            _navigateBack.value = false
            return
        }
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
            // The image is drawn width-fit, so the crop math works against the
            // drawn size (frame width x matching height), not the bitmap's
            // intrinsic pixel size.
            val drawnSize = Size(
                frameSize.width,
                frameSize.width / currentImageAspectRatio()
            )
            val cropRect = CropMath.normalizedCropRect(
                scale = _uiState.value.scale,
                offset = _uiState.value.offset,
                frameSize = frameSize,
                displayedImageSize = drawnSize
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
