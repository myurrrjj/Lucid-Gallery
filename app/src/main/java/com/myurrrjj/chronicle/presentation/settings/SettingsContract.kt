package com.myurrrjj.chronicle.presentation.settings

import com.myurrrjj.chronicle.core.presentation.mvi.ViewEvent
import com.myurrrjj.chronicle.core.presentation.mvi.ViewSideEffect
import com.myurrrjj.chronicle.core.presentation.mvi.ViewState
import com.myurrrjj.chronicle.domain.model.AppSettings
import com.myurrrjj.chronicle.domain.model.GalleryViewMode

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