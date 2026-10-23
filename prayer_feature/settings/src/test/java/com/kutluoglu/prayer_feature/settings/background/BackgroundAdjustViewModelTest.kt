package com.kutluoglu.prayer_feature.settings.background

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer_feature.settings.MainCoroutineRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@OptIn(ExperimentalCoroutinesApi::class)
@Execution(value = ExecutionMode.SAME_THREAD)
@ExtendWith(MainCoroutineRule::class)
class BackgroundAdjustViewModelTest {

    private lateinit var locationsCoordinator: LocationsCoordinator
    private lateinit var backgroundImageStore: BackgroundImageStore
    private lateinit var viewModel: BackgroundAdjustViewModel

    private val locationId = "loc1"
    private val imageUri: android.net.Uri = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        locationsCoordinator = mockk()
        coEvery { locationsCoordinator.setBackgroundPath(any(), any()) } returns Unit
        // The orphan-cleanup path reads the current state; default to empty.
        coEvery { locationsCoordinator.observeState() } returns flowOf(LocationsState())
        backgroundImageStore = mockk()
        viewModel = BackgroundAdjustViewModel(
            locationId = locationId,
            imageUri = imageUri,
            locationsCoordinator = locationsCoordinator,
            backgroundImageStore = backgroundImageStore
        )
    }

    @Test
    fun `Confirm saves baked crop and persists path then emits navigate back`() = runTest {
        // Arrange
        coEvery {
            backgroundImageStore.saveBakedCrop(any(), any(), any())
        } returns "/data/bg.jpg"
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.Confirm)

        // Assert
        coVerify {
            backgroundImageStore.saveBakedCrop(any(), any(), locationId)
        }
        coVerify { locationsCoordinator.setBackgroundPath(locationId, "/data/bg.jpg") }
        viewModel.navigateBack.test {
            // UnconfinedTestDispatcher runs the confirm coroutine eagerly, so
            // the flow already holds the final value when the test subscribes.
            assertThat(awaitItem()).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Confirm with failed save does not persist or navigate`() = runTest {
        // Arrange
        coEvery { backgroundImageStore.saveBakedCrop(any(), any(), any()) } returns null
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.Confirm)

        // Assert
        coVerify(exactly = 0) { locationsCoordinator.setBackgroundPath(any(), any()) }
        viewModel.navigateBack.test {
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnTransform updates scale and offset`() = runTest {
        // Act: 1:1 image in 1:1 frame at scale 2 → image spans 2x2, so offset
        // (-10, -20) is within [-2, 0]... no: clamps to (-2, -2)? Verify via impl:
        // imageSize = scale = 2 → offset clamps to [-2, 0] x [-2, 0].
        viewModel.onEvent(
            BackgroundAdjustEvent.OnTransform(
                scale = 2f,
                offset = androidx.compose.ui.geometry.Offset(-10f, -20f)
            )
        )

        // Assert: clamped to the image bounds
        val state = viewModel.uiState.value
        assertThat(state.scale).isEqualTo(2f)
        assertThat(state.offset.x).isEqualTo(-2f)
        assertThat(state.offset.y).isEqualTo(-2f)
    }

    @Test
    fun `OnDoubleTap resets zoom and offset`() = runTest {
        // Arrange
        viewModel.onEvent(BackgroundAdjustEvent.OnTransform(scale = 3f, offset = androidx.compose.ui.geometry.Offset(-50f, -50f)))

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.OnDoubleTap)

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.scale).isEqualTo(1f)
        assertThat(state.offset).isEqualTo(androidx.compose.ui.geometry.Offset.Zero)
    }

    @Test
    fun `isSaving is true while saving and false after`() = runTest {
        // Arrange: suspend the save so we can observe the in-flight state
        coEvery {
            backgroundImageStore.saveBakedCrop(any(), any(), any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            "/data/bg.jpg"
        }
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.Confirm)

        // Assert
        assertThat(viewModel.uiState.value.isSaving).isTrue()
        advanceTimeBy(200)
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }
}
