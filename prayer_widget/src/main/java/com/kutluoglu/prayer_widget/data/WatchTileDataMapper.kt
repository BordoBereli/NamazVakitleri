package com.kutluoglu.prayer_widget.data

import com.kutluoglu.wear.shared.model.WatchPrayer
import com.kutluoglu.wear.shared.model.WatchTileData
import org.koin.core.annotation.Factory

@Factory
class WatchTileDataMapper {

    fun map(data: WidgetData): WatchTileData = WatchTileData(
        locationName = data.locationName,
        nextPrayerName = data.nextPrayerName,
        nextPrayerEpochMillis = data.nextPrayerEpochMillis,
        currentPrayerEpochMillis = data.currentPrayerEpochMillis,
        isJumuah = data.isJumuah,
        prayers = data.prayers.map {
            WatchPrayer(
                name = it.name,
                time = it.time,
                isNext = it.isNext,
                isJumuah = it.isJumuah
            )
        },
        syncedAtEpochMillis = System.currentTimeMillis()
    )
}
