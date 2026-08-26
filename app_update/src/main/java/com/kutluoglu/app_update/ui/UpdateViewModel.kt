package com.kutluoglu.app_update.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kutluoglu.app_update.data.InstallSourceDetector
import com.kutluoglu.app_update.domain.model.UpdateDecision
import com.kutluoglu.app_update.domain.usecase.CheckForUpdateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class UpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val installSourceDetector: InstallSourceDetector,
    private val updateUrlOpener: UpdateUrlOpener,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.NoUpdate)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var checkInFlight = false

    fun checkForUpdate() {
        if (checkInFlight) return
        checkInFlight = true
        viewModelScope.launch {
            val decision = checkForUpdateUseCase()
            _uiState.value = when (decision) {
                is UpdateDecision.ForceUpdate -> UpdateUiState.ForceUpdate(decision.info)
                is UpdateDecision.OptionalUpdate -> UpdateUiState.OptionalUpdate(decision.info)
                UpdateDecision.NoUpdate -> UpdateUiState.NoUpdate
            }
            checkInFlight = false
        }
    }

    fun onOptionalUpdateDismissed() {
        _uiState.value = UpdateUiState.NoUpdate
    }

    fun onUpdateClicked() {
        val info = (_uiState.value as? UpdateUiState.ForceUpdate)?.info
            ?: (_uiState.value as? UpdateUiState.OptionalUpdate)?.info
            ?: return
        val url = if (installSourceDetector.isPlayStoreInstall()) {
            installSourceDetector.getPlayStoreUrl()
        } else {
            installSourceDetector.getDirectDownloadUrl(info)
        }
        var opened = updateUrlOpener.open(url)
        if (!opened && installSourceDetector.isPlayStoreInstall()) {
            opened = updateUrlOpener.open(installSourceDetector.getPlayStoreWebUrl())
        }
        if (!opened) {
            val current = _uiState.value
            if (current is UpdateUiState.ForceUpdate) {
                _uiState.value = current.copy(urlOpenFailed = true)
            }
        }
    }
}
