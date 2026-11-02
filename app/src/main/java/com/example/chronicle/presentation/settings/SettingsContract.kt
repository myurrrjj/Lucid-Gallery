package com.example.chronicle.presentation.settings

import com.example.chronicle.core.presentation.mvi.ViewEvent
import com.example.chronicle.core.presentation.mvi.ViewSideEffect
import com.example.chronicle.core.presentation.mvi.ViewState
import com.example.chronicle.domain.model.AppSettings
import com.example.chronicle.domain.model.GalleryViewMode

data class SettingsState(
    val isLoading: Boolean = true,
    val appSettings: AppSettings = AppSettings()
) : ViewState

sealed class SettingsEvent : ViewEvent {
    data class ToggleAutoPlay(val isEnabled: Boolean) : SettingsEvent()
    data class ChangeDefaultViewMode(val viewMode: GalleryViewMode) : SettingsEvent()
    object CloseSettings : SettingsEvent()
}

sealed class SettingsEffect : ViewSideEffect {
    object DismissSheet : SettingsEffect()
}