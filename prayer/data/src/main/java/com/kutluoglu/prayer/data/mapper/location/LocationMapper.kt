package com.kutluoglu.prayer.data.mapper.location

import com.kutluoglu.prayer.data.mapper.Mapper
import com.kutluoglu.prayer.data.model.LocationDataModel
import com.kutluoglu.prayer.model.location.LocationData
import org.koin.core.annotation.Factory

/**
 * Created by F.K. on 30.11.2025.
 *
 */

@Factory
class LocationMapper() : Mapper<LocationDataModel, LocationData>  {
    override fun mapToDomain(type: LocationDataModel) = LocationData(
        latitude = type.latitude,
        longitude = type.longitude,
        country = type.country,
        countryCode = type.countryCode,
        city = type.city,
        county = type.county
    )

    override fun mapFromDomain(type: LocationData) = LocationDataModel(
        latitude = type.latitude,
        longitude = type.longitude,
        country = type.country,
        countryCode = type.countryCode,
        city = type.city,
        county = type.county
    )
}