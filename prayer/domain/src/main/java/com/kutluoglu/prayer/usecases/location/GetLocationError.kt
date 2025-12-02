package com.kutluoglu.prayer.usecases.location

/**
 * Created by F.K. on 2.12.2025.
 *
 */
sealed class GetLocationError {
    object NOT_FOUND : GetLocationError()
    data class UNKNOWN(val message: String?) : GetLocationError()
}
