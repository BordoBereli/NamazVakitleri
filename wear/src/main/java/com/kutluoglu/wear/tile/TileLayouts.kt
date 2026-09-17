package com.kutluoglu.wear.tile

import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.LayoutString
import com.kutluoglu.wear.R
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TileLayouts {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun nextPrayerPage(
        scope: MaterialScope,
        data: WatchTileData,
        countdown: String,
        ring: Float
    ): LayoutElement = scope.primaryLayout(
        mainSlot = {
            Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                .addContent(
                    circularProgressIndicator(
                        staticProgress = ring,
                        strokeWidth = CircularProgressIndicatorDefaults.LARGE_STROKE_WIDTH,
                    )
                )
                .addContent(
                    Column.Builder()
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            text(
                                LayoutString(data.locationName),
                                typography = Typography.TITLE_SMALL,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(data.nextPrayerName),
                                typography = Typography.TITLE_LARGE,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(nextPrayerTime(data)),
                                typography = Typography.BODY_MEDIUM,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(countdown),
                                typography = Typography.BODY_SMALL,
                            )
                        )
                        .apply {
                            if (isStale(data.syncedAtEpochMillis, System.currentTimeMillis())) {
                                addContent(
                                    text(
                                        LayoutString(
                                            scope.context.getString(
                                                R.string.wear_tile_last_synced,
                                                formatTime(data.syncedAtEpochMillis)
                                            )
                                        ),
                                        typography = Typography.BODY_EXTRA_SMALL,
                                        color = scope.colorScheme.onSurfaceVariant,
                                    )
                                )
                            }
                        }
                        .build()
                )
                .build()
        }
    )

    fun prayerListPage(scope: MaterialScope, data: WatchTileData): LayoutElement = scope.primaryLayout(
        titleSlot = {
            text(LayoutString(data.locationName), typography = Typography.TITLE_SMALL)
        },
        mainSlot = {
            Column.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .apply {
                    data.prayers.forEach { prayer ->
                        addContent(prayerRow(prayer))
                    }
                }
                .build()
        }
    )

    fun emptyPage(scope: MaterialScope): LayoutElement = scope.primaryLayout(
        mainSlot = {
            Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                .addContent(
                    Column.Builder()
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            text(
                                LayoutString(scope.context.getString(R.string.wear_tile_no_data_title)),
                                typography = Typography.TITLE_SMALL,
                                alignment = TEXT_ALIGN_CENTER,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(scope.context.getString(R.string.wear_tile_no_data_subtitle)),
                                typography = Typography.BODY_SMALL,
                                alignment = TEXT_ALIGN_CENTER,
                            )
                        )
                        .build()
                )
                .build()
        }
    )

    private fun MaterialScope.prayerRow(prayer: WatchPrayer): LayoutElement {
        val typography = if (prayer.isNext) Typography.TITLE_SMALL else Typography.BODY_MEDIUM
        val color = if (prayer.isNext) colorScheme.primary else colorScheme.onSurface
        return Row.Builder()
            .setWidth(DimensionBuilders.expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(text(LayoutString(prayer.name), typography = typography, color = color))
            .addContent(Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
            .addContent(text(LayoutString(prayer.time), typography = typography, color = color))
            .build()
    }

    private fun nextPrayerTime(data: WatchTileData): String =
        data.prayers.firstOrNull { it.isNext }?.time.orEmpty()

    internal fun isStale(syncedAtEpochMillis: Long, nowEpochMillis: Long): Boolean =
        nowEpochMillis - syncedAtEpochMillis > 5 * 60_000L

    internal fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
}
