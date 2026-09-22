package com.kutluoglu.prayer_location.data.legacy

import com.kutluoglu.prayer.model.location.LocationData
import org.koin.core.annotation.Factory

/**
 * Maps between the legacy persistence DTO and the domain [LocationData].
 */
@Factory
class LegacyLocationMapper : LegacyMapper<LegacyLocationDataModel, LocationData> {
    override fun mapToDomain(type: LegacyLocationDataModel) = LocationData(
        latitude = type.latitude,
        longitude = type.longitude,
        country = type.country,
        countryCode = type.countryCode,
        city = type.city,
        county = type.county,
        timeZoneId = type.timeZoneId
    )

    override fun mapFromDomain(type: LocationData) = LegacyLocationDataModel(
        latitude = type.latitude,
        longitude = type.longitude,
        country = type.country,
        countryCode = type.countryCode,
        city = type.city,
        county = type.county,
        timeZoneId = type.timeZoneId
    )
}
