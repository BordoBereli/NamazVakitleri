package com.kutluoglu.app_update.domain.model

sealed interface UpdateDecision {
    data object NoUpdate : UpdateDecision
    data class ForceUpdate(val info: UpdateInfo) : UpdateDecision
    data class OptionalUpdate(val info: UpdateInfo) : UpdateDecision
}
