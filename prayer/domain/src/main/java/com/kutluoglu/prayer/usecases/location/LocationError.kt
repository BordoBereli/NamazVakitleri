package com.kutluoglu.prayer.usecases.location

/**
 * Created by F.K. on 2.12.2025.
 *
 */
sealed class LocationError {
    data class NOT_FOUND(val message: String = "NOT_FOUND") : LocationError()
    data class UNKNOWN(val message: String?) : LocationError()
}
