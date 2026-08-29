package com.kutluoglu.prayer_notifications.manager

import android.os.Looper
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class TestVolumePlayer : SimpleBasePlayer(Looper.myLooper()!!) {

    private var simulatedDeviceVolume = 0
    private var simulatedDeviceMuted = false

    private var state = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlayWhenReady(false, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaybackParameters(PlaybackParameters(DEFAULT_PLAYBACK_SPEED))
        .setDeviceInfo(DeviceInfo.Builder(DeviceInfo.PLAYBACK_TYPE_LOCAL).build())
        .setDeviceVolume(0)
        .setIsDeviceMuted(false)
        .build()

    private fun handleStateUpdate(block: State.Builder.() -> Unit): ListenableFuture<*> {
        state = state.buildUpon().apply(block).build()
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun getState(): State = state.buildUpon()
        .setDeviceVolume(simulatedDeviceVolume)
        .setIsDeviceMuted(simulatedDeviceMuted)
        .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean) = handleStateUpdate {
        setPlayWhenReady(playWhenReady, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }

    override fun handlePrepare() = handleStateUpdate {
        if (state.timeline.isEmpty) {
            setPlaybackState(STATE_ENDED)
        } else {
            setPlaybackState(STATE_READY)
        }
    }

    override fun handleStop() = handleStateUpdate {
        setPlaybackState(STATE_IDLE)
            .setTotalBufferedDurationMs(PositionSupplier.ZERO)
            .setIsLoading(false)
    }

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSetMediaItems(
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ) = handleStateUpdate {
        if (mediaItems.isEmpty() && playbackState != STATE_IDLE) {
            setPlaybackState(STATE_ENDED)
        }
        setPlaylist(mediaItems.map { MediaItemData.Builder(it.mediaId).setMediaItem(it).build() })
            .setContentPositionMs(startPositionMs)
            .setCurrentMediaItemIndex(startIndex)
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean) = handleStateUpdate {
        setShuffleModeEnabled(shuffleModeEnabled)
    }

    override fun handleSetRepeatMode(repeatMode: Int) = handleStateUpdate {
        setRepeatMode(repeatMode)
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters) =
        handleStateUpdate { setPlaybackParameters(playbackParameters) }

    override fun handleSetTrackSelectionParameters(
        trackSelectionParameters: TrackSelectionParameters
    ) = handleStateUpdate { setTrackSelectionParameters(trackSelectionParameters) }

    override fun handleSetVolume(
        volume: Float,
        volumeOperationType: @androidx.media3.common.C.VolumeOperationType Int
    ) = handleStateUpdate { setVolume(volume) }

    override fun handleSetDeviceVolume(deviceVolume: Int, flags: Int): ListenableFuture<*> {
        simulatedDeviceVolume = deviceVolume
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleIncreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        simulatedDeviceVolume += 1
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleDecreaseDeviceVolume(flags: Int): ListenableFuture<*> {
        simulatedDeviceVolume = maxOf(0, simulatedDeviceVolume - 1)
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSetDeviceMuted(isDeviceMuted: Boolean, flags: Int): ListenableFuture<*> {
        simulatedDeviceMuted = isDeviceMuted
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    fun setPlaybackState(playbackState: @Player.State Int) {
        state = state.buildUpon().setPlaybackState(playbackState).build()
        invalidateState()
    }

    fun setPlayerError(playerError: PlaybackException?) {
        val builder = state.buildUpon()
        if (playerError != null) {
            builder.setPlaybackState(STATE_IDLE)
        }
        state = builder.setPlayerError(playerError).build()
        invalidateState()
    }

    companion object {
        private const val DEFAULT_PLAYBACK_SPEED = 1f
    }
}