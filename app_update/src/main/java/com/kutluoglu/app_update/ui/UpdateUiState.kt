package com.kutluoglu.app_update.ui

import com.kutluoglu.app_update.domain.model.UpdateInfo

sealed interface UpdateUiState {
    data object NoUpdate : UpdateUiState
    data class OptionalUpdate(val info: UpdateInfo) : UpdateUiState
    data class ForceUpdate(
        val info: UpdateInfo,
        val urlOpenFailed: Boolean = false,
    ) : UpdateUiState
}
