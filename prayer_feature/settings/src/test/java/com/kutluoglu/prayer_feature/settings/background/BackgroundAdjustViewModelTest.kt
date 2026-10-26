package com.kutluoglu.prayer_feature.settings.background

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kutluoglu.prayer_location.LocationsCoordinator
import com.kutluoglu.prayer_location.data.BackgroundImageStore
import com.kutluoglu.prayer_location.data.LocationsState
import com.kutluoglu.prayer.model.location.LocationData
import com.kutluoglu.prayer.model.location.LocationEntry
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
    fun `Confirm on replace deletes the previous file when unreferenced`() = runTest {
        // Arrange: the location currently points at /data/old.jpg; nothing else
        // references it. The new save produces /data/bg.jpg. The coordinator
        // mock is stateful: setBackgroundPath mutates the state that
        // observeState() returns, modelling the real store.
        val entries = mutableListOf(
            LocationEntry(
                id = locationId,
                location = LocationData(
                    latitude = 41.0, longitude = 29.0,
                    country = "TR", countryCode = "TR",
                    city = "Istanbul", county = null
                ),
                displayName = "Istanbul",
                backgroundPath = "/data/old.jpg"
            )
        )
        coEvery { locationsCoordinator.observeState() } answers { flowOf(LocationsState(entries = entries.toList())) }
        coEvery { locationsCoordinator.setBackgroundPath(any(), any()) } coAnswers { params ->
            @Suppress("UNCHECKED_CAST")
            val id = params.invocation.args[0] as String
            @Suppress("UNCHECKED_CAST")
            val path = params.invocation.args[1] as String?
            entries.removeAll { it.id == id }
            if (path != null) {
                entries.add(
                    LocationEntry(
                        id = id,
                        location = LocationData(
                            latitude = 41.0, longitude = 29.0,
                            country = "TR", countryCode = "TR",
                            city = "Istanbul", county = null
                        ),
                        displayName = "Istanbul",
                        backgroundPath = path
                    )
                )
            }
            Unit
        }
        coEvery {
            backgroundImageStore.saveBakedCrop(any(), any(), any())
        } returns "/data/bg.jpg"
        coEvery { backgroundImageStore.deleteBackground(any()) } returns Unit
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.Confirm)

        // Assert: the previous file is deleted (cleanup runs on a real IO
        // thread; block briefly so it completes before verifying).
        Thread.sleep(300)
        coVerify { backgroundImageStore.deleteBackground("/data/old.jpg") }
    }

    @Test
    fun `Confirm on replace keeps the previous file when another location references it`() = runTest {
        // Arrange: loc2 shares /data/old.jpg with loc1 — the file must survive.
        // Stateful mock as above; after the replace, loc2 still references the
        // old file.
        val entries = mutableListOf(
            LocationEntry(
                id = locationId,
                location = LocationData(
                    latitude = 41.0, longitude = 29.0,
                    country = "TR", countryCode = "TR",
                    city = "Istanbul", county = null
                ),
                displayName = "Istanbul",
                backgroundPath = "/data/old.jpg"
            ),
            LocationEntry(
                id = "loc2",
                location = LocationData(
                    latitude = 39.9, longitude = 32.8,
                    country = "TR", countryCode = "TR",
                    city = "Ankara", county = null
                ),
                displayName = "Ankara",
                backgroundPath = "/data/old.jpg"
            )
        )
        coEvery { locationsCoordinator.observeState() } answers { flowOf(LocationsState(entries = entries.toList())) }
        coEvery { locationsCoordinator.setBackgroundPath(any(), any()) } coAnswers { params ->
            @Suppress("UNCHECKED_CAST")
            val id = params.invocation.args[0] as String
            @Suppress("UNCHECKED_CAST")
            val path = params.invocation.args[1] as String?
            entries.removeAll { it.id == id }
            if (path != null) {
                entries.add(
                    LocationEntry(
                        id = id,
                        location = LocationData(
                            latitude = 41.0, longitude = 29.0,
                            country = "TR", countryCode = "TR",
                            city = "Istanbul", county = null
                        ),
                        displayName = "Istanbul",
                        backgroundPath = path
                    )
                )
            }
            Unit
        }
        coEvery {
            backgroundImageStore.saveBakedCrop(any(), any(), any())
        } returns "/data/bg.jpg"
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(BackgroundAdjustEvent.Confirm)

        // Assert: the shared file is NOT deleted
        Thread.sleep(300)
        coVerify(exactly = 0) { backgroundImageStore.deleteBackground(any()) }
    }

    @Test
    fun `Confirm with failed save does not persist or navigate`() = runTest {        // Arrange
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
        // Arrange: 1:1 image in a 400x400 frame; at scale 2 the drawn image is
        // 800x800 px, so the top-left corner may sit anywhere in [-400, 0].
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act
        viewModel.onEvent(
            BackgroundAdjustEvent.OnTransform(
                scale = 2f,
                offset = androidx.compose.ui.geometry.Offset(-10f, -20f)
            )
        )

        // Assert: a valid transform is kept as-is
        val state = viewModel.uiState.value
        assertThat(state.scale).isEqualTo(2f)
        assertThat(state.offset.x).isEqualTo(-10f)
        assertThat(state.offset.y).isEqualTo(-20f)
    }

    @Test
    fun `OnTransform clamps offset so the frame stays covered`() = runTest {
        // Arrange: at scale 2 the 800x800 image can pan 400 px in each axis.
        viewModel.frameSize = androidx.compose.ui.geometry.Size(400f, 400f)
        viewModel.displayedImageSize = androidx.compose.ui.geometry.Size(800f, 800f)

        // Act: pan far past the bounds in both axes
        viewModel.onEvent(
            BackgroundAdjustEvent.OnTransform(
                scale = 2f,
                offset = androidx.compose.ui.geometry.Offset(-1000f, 1000f)
            )
        )

        // Assert: clamps to the bottom-right corner of the pannable range
        val state = viewModel.uiState.value
        assertThat(state.offset.x).isEqualTo(-400f)
        assertThat(state.offset.y).isEqualTo(0f)
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
