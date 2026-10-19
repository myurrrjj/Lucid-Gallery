package com.example.jetpackcomposegalleryapp.presentation.gallery.screens.mainGridScreen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ContextualSelectionBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.GalleryTopBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopAppBar(
    selectionMode: Boolean,
    selectedMediaCount: Int,
    totalDisplayedCount: Int,
    mediaCount: Int,
    scrollBehaviour: TopAppBarScrollBehavior,
    onEvent: (GalleryEvent) -> Unit
) {
    if (selectionMode) {
        ContextualSelectionBar(
            selectedCount = selectedMediaCount,
            totalDisplayedCount = totalDisplayedCount,
            onCloseClick = { onEvent(GalleryEvent.ExitSelectionMode) },
            onSelectAllClick = { onEvent(GalleryEvent.SelectAll) },
            onClearSelectionClick = { onEvent(GalleryEvent.ClearSelection) },
            scrollBehavior = scrollBehaviour
        )
    } else {
        GalleryTopBar(
            onSearchClick = { onEvent(GalleryEvent.onSearchClicked) },
            mediaCount = mediaCount,
            scrollBehaviour = scrollBehaviour,
            onSettingsClick = { onEvent(GalleryEvent.OnSettingsClicked) }
        )
    }
}