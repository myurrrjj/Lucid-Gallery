package com.myurrrjj.chronicle.presentation.settings

import androidx.lifecycle.viewModelScope
import com.myurrrjj.chronicle.core.presentation.mvi.BaseViewModel
import com.myurrrjj.chronicle.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : BaseViewModel<SettingsEvent, SettingsState, SettingsEffect>() {

    init {
        observeSettings()
    }

    override fun createInitialState(): SettingsState = SettingsState()

    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleAutoPlay -> {
                viewModelScope.launch {
                    settingsRepository.updateAutoPlayVideos(event.isEnabled)
                }
            }
            is SettingsEvent.ChangeDefaultViewMode -> {
                viewModelScope.launch {
                    settingsRepository.updateDefaultGalleryViewMode(event.viewMode)
                }
            }
            SettingsEvent.CloseSettings -> {
                setEffect { SettingsEffect.DismissSheet }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings
                .onStart {
                    setState { copy(isLoading = true) }
                }
                .catch {
                    setState { copy(isLoading = false) }
                }
                .collect { newSettings ->
                    setState {
                        copy(
                            isLoading = false,
                            appSettings = newSettings
                        )
                    }
                }
        }
    }
}