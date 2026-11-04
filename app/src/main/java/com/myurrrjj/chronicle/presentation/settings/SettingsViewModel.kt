package com.myurrrjj.chronicle.presentation.settings

import androidx.lifecycle.viewModelScope
import com.myurrrjj.chronicle.core.indexing.FaceIndexingController
import com.myurrrjj.chronicle.core.presentation.mvi.BaseViewModel
import com.myurrrjj.chronicle.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val faceIndexingController: FaceIndexingController

) : BaseViewModel<SettingsEvent, SettingsState, SettingsEffect>() {

    init {
        observeSettings()
        observeFaceIndexing()
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

            SettingsEvent.OpenFaceIndexingSheet -> {
                setState { copy(showFaceIndexingSheet = true) }
            }
            SettingsEvent.DismissFaceIndexingSheet -> {
                setState { copy(showFaceIndexingSheet = false) }
            }
            is SettingsEvent.StartFaceIndexing -> {
                setState { copy(showFaceIndexingSheet = false) }
                viewModelScope.launch {
                    faceIndexingController.start(event.options)
                }
            }
            is SettingsEvent.ChangeFaceIndexingOptions -> {

                viewModelScope.launch {
                    faceIndexingController.updateOptions(event.options)
                }
            }
            SettingsEvent.PauseFaceIndexing -> {
                viewModelScope.launch { faceIndexingController.pause() }
            }
            SettingsEvent.ResumeFaceIndexing -> {
                viewModelScope.launch { faceIndexingController.resume() }
            }
            SettingsEvent.ForceStopFaceIndexing -> {
                viewModelScope.launch { faceIndexingController.forceStop() }
            }
            SettingsEvent.ResetFaceIndexing -> {
                viewModelScope.launch { faceIndexingController.reset() }
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
    private fun observeFaceIndexing() {
        viewModelScope.launch {
            faceIndexingController.progress.collect { progress ->
                setState { copy(faceIndexing = progress) }
            }
        }
    }
}