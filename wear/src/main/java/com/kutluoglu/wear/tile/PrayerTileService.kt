package com.kutluoglu.wear.tile

import android.annotation.SuppressLint
import android.content.Context
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.protolayout.expression.VersionBuilders
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.createMaterialScope
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.kutluoglu.wear.R
import com.kutluoglu.wear.data.TileDataRepository
import com.kutluoglu.wear.shared.data.WatchCountdownCalculator
import com.kutluoglu.wear.shared.model.WatchTileData
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrayerTileService(
    private val repositoryOverride: TileDataRepository? = null
) : Material3TileService(), KoinComponent {

    private val repository: TileDataRepository by inject()

    private val activeRepository: TileDataRepository
        get() = repositoryOverride ?: repository

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val data = activeRepository.getTileData()
        if (data == null) {
            activeRepository.requestSync()
        }
        return buildTile(this, data)
    }

    @SuppressLint("RestrictedApi")
    internal suspend fun buildTileForTest(
        requestParams: TileRequest,
        context: Context
    ): Tile {
        val data = activeRepository.getTileData()
        val scope = createMaterialScope(
            context = context,
            deviceConfiguration = requestParams.deviceConfiguration ?: defaultDeviceParameters(),
            allowDynamicTheme = false,
            defaultColorScheme = ColorScheme(),
        )
        return buildTile(scope, data)
    }

    private fun buildTile(scope: MaterialScope, data: WatchTileData?): Tile {
        val now = System.currentTimeMillis()
        // 1f - progress => the ring depletes as time passes, showing the
        // REMAINING time until the next prayer (a true countdown ring).
        val page = if (data != null) {
            TileLayouts.nextPrayerPage(scope, data, countdownText(scope, data, now), 1f - ringProgress(data, now))
        } else {
            TileLayouts.emptyPage(scope)
        }
        // Single page: only the ring and its data. This Samsung watch's renderer
        // shows a single page and does not scroll between timeline entries.
        val timeline = Timeline.Builder()
            .addTimelineEntry(
                TimelineEntry.Builder()
                    .setLayout(Layout.Builder().setRoot(page).build())
                    .build()
            )
            .build()
        return Tile.Builder()
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(timeline)
            .build()
    }

    private fun countdownText(scope: MaterialScope, data: WatchTileData, now: Long): String =
        WatchCountdownCalculator.countdownText(
            nextPrayerEpochMillis = data.nextPrayerEpochMillis,
            nowEpochMillis = now,
            hourShort = scope.context.getString(R.string.wear_countdown_hour_short),
            minuteShort = scope.context.getString(R.string.wear_countdown_minute_short),
        )

    private fun ringProgress(data: WatchTileData, now: Long): Float =
        WatchCountdownCalculator.ringProgress(
            currentPrayerEpochMillis = data.currentPrayerEpochMillis,
            nextPrayerEpochMillis = data.nextPrayerEpochMillis,
            nowEpochMillis = now,
        )

    @SuppressLint("RestrictedApi")
    private fun defaultDeviceParameters(): DeviceParameters =
        DeviceParameters.Builder()
            .setScreenWidthDp(192)
            .setScreenHeightDp(192)
            .setScreenDensity(1f)
            .setRendererSchemaVersion(VersionBuilders.VersionInfo.CURRENT)
            .build()
}
