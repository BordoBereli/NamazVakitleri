package com.kutluoglu.app_update.domain.usecase

import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.repository.UpdateRepository

class CheckForUpdateUseCase(
    private val repository: UpdateRepository,
    private val currentVersionCode: Int,
) {

    suspend operator fun invoke(): UpdateDecision {
        val info = repository.getUpdateInfo() ?: return UpdateDecision.NoUpdate
        return when {
            info.forceVersionCodes.contains(currentVersionCode) -> UpdateDecision.ForceUpdate(info)
            info.optionalVersionCodes.contains(currentVersionCode) -> UpdateDecision.OptionalUpdate(info)
            currentVersionCode < info.minVersionCode -> UpdateDecision.ForceUpdate(info)
            currentVersionCode < info.latestVersionCode -> UpdateDecision.OptionalUpdate(info)
            else -> UpdateDecision.NoUpdate
        }
    }
}
