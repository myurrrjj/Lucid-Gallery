package com.example.jetpackcomposegalleryapp.presentation.search.contract

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewSideEffect
import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewState
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset

data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isIndexing : Boolean = true,
    val indexingProgress: Float = 0f,
    val currentViewMode: GalleryViewMode = GalleryViewMode.DAY,
    val groupedResults: Map<String, List<MediaAsset>> = emptyMap()
) : ViewState

sealed class SearchEvent : ViewEvent {
    data class OnQueryChanged(val query: String) : SearchEvent()
    object ClearSearch : SearchEvent()
    data class OnMediaClicked(val mediaId: Long) : SearchEvent()
    object OnBackClicked : SearchEvent()
    data class ChangeViewMode(val viewMode: GalleryViewMode) : SearchEvent()
}

sealed class SearchEffect : ViewSideEffect {
    data class NavigateToDetail(val mediaId: Long) : SearchEffect()
    object NavigateBack : SearchEffect()
}