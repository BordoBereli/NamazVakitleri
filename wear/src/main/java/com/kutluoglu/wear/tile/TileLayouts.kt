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
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.ProgressIndicatorColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.circularProgressIndicator
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.LayoutString
import com.kutluoglu.wear.R
import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TileLayouts {

    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Ring diameter in dp. A fixed DpProp (not expand()) is required so the ring
    // renders full-size even on renderers without dashed-arc support, where an
    // expand() size silently falls back to the tiny 52dp default.
    private const val RING_SIZE_DP = 168f

    // Brand gold palette (matches the phone widget's gold-on-dark).
    private val Gold: LayoutColor = LayoutColor(0xFFFFD700.toInt(), null)
    private val GoldDim: LayoutColor = LayoutColor(0xFFB8A24A.toInt(), null)
    private val TrackColor: LayoutColor = LayoutColor(0xFF33333A.toInt(), null)
    private val OnDark: LayoutColor = LayoutColor(0xFFFFFFFF.toInt(), null)
    private val OnDarkMuted: LayoutColor = LayoutColor(0xFFB0B0B8.toInt(), null)

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
                        colors = ProgressIndicatorColors(
                            indicatorColor = Gold,
                            trackColor = TrackColor,
                            trackOverflowColor = TrackColor,
                        ),
                        size = DimensionBuilders.dp(RING_SIZE_DP),
                    )
                )
                .addContent(
                    Column.Builder()
                        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                        .addContent(
                            text(
                                LayoutString(data.locationName),
                                typography = Typography.LABEL_SMALL,
                                color = OnDarkMuted,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(data.nextPrayerName),
                                typography = Typography.TITLE_MEDIUM,
                                color = Gold,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(nextPrayerTime(data)),
                                typography = Typography.DISPLAY_SMALL,
                                color = OnDark,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(countdown),
                                typography = Typography.TITLE_SMALL,
                                color = Gold,
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
                                        color = OnDarkMuted,
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
            text(LayoutString(data.locationName), typography = Typography.LABEL_SMALL, color = OnDarkMuted)
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
                                color = OnDark,
                                alignment = TEXT_ALIGN_CENTER,
                            )
                        )
                        .addContent(
                            text(
                                LayoutString(scope.context.getString(R.string.wear_tile_no_data_subtitle)),
                                typography = Typography.BODY_SMALL,
                                color = OnDarkMuted,
                                alignment = TEXT_ALIGN_CENTER,
                            )
                        )
                        .build()
                )
                .build()
        }
    )

    private fun MaterialScope.prayerRow(prayer: WatchPrayer): LayoutElement {
        val isNext = prayer.isNext
        val typography = if (isNext) Typography.TITLE_SMALL else Typography.BODY_MEDIUM
        val color = if (isNext) Gold else OnDarkMuted
        val row = Row.Builder()
            .setWidth(DimensionBuilders.expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(text(LayoutString(prayer.name), typography = typography, color = color))
            .addContent(Spacer.Builder().setWidth(DimensionBuilders.dp(8f)).build())
            .addContent(text(LayoutString(prayer.time), typography = typography, color = color))
        if (isNext) {
            row.setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(GoldDim.prop)
                            .setCorner(
                                ModifiersBuilders.Corner.Builder()
                                    .setRadius(DimensionBuilders.dp(16f))
                                    .build()
                            )
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(DimensionBuilders.dp(8f))
                            .setEnd(DimensionBuilders.dp(8f))
                            .setTop(DimensionBuilders.dp(2f))
                            .setBottom(DimensionBuilders.dp(2f))
                            .build()
                    )
                    .build()
            )
        }
        return row.build()
    }

    private fun nextPrayerTime(data: WatchTileData): String =
        data.prayers.firstOrNull { it.isNext }?.time
            ?: formatEpochTime(data.nextPrayerEpochMillis)

    private fun formatEpochTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)

    internal fun isStale(syncedAtEpochMillis: Long, nowEpochMillis: Long): Boolean =
        nowEpochMillis - syncedAtEpochMillis > 5 * 60_000L

    internal fun formatTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(timeFormatter)
}
