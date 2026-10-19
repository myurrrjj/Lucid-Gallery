package com.example.jetpackcomposegalleryapp.presentation.gallery.screens.mainGridScreen

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonDetailView
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun SharedTransitionScope.GalleryDetailContent(
    target: ScreenTarget,
    selectionMode: Boolean,
    groupedMedia: ImmutableMap<String, ImmutableList<MediaAsset>>,
    currentViewMode: GalleryViewMode,
    selectedMediaIds: ImmutableSet<Long>,
    onEvent: (GalleryEvent) -> Unit,
    galleryToDetailScope: AnimatedContentScope,
    tabAnimatedVisibilityScope: AnimatedContentScope
) {
    if (target.album != null) {
        AlbumDetailView(
            album = target.album,
            selectionMode = selectionMode,
            groupedMedia = groupedMedia,
            currentViewMode = currentViewMode,
            onBackClick = { onEvent(GalleryEvent.CloseAlbum) },
            onMediaClick = { id -> onEvent(GalleryEvent.MediaClicked(id)) },
            modifier = Modifier.padding(top = 8.dp),
            selectedMediaIds = selectedMediaIds,
            onToggleSelection = { mediaId ->
                onEvent(GalleryEvent.ToggleMediaSelection(mediaId))
            },
            onEnterSelectionMode = {
                onEvent(GalleryEvent.EnterSelectionMode)
            },
            onViewModeChanged = { newMode ->
                onEvent(GalleryEvent.ChangeViewMode(newMode))
            },
            albumAnimatedVisibilityScope = galleryToDetailScope,
            tabAnimatedVisibilityScope = tabAnimatedVisibilityScope
        )
    } else if (target.person != null) {
        PersonDetailView(
            cluster = target.person,
            selectionMode = selectionMode,
            groupedMedia = groupedMedia,
            currentViewMode = currentViewMode,
            onBackClick = { onEvent(GalleryEvent.ClosePerson) },
            onUpdateName = { id, name ->
                onEvent(GalleryEvent.UpdatePersonName(id, name))
            },
            onMediaClick = { id -> onEvent(GalleryEvent.MediaClicked(id)) },
            modifier = Modifier.padding(top = 8.dp),
            selectedMediaIds = selectedMediaIds,
            onToggleSelection = { mediaId ->
                onEvent(GalleryEvent.ToggleMediaSelection(mediaId))
            },
            onEnterSelectionMode = { onEvent(GalleryEvent.EnterSelectionMode) },
            onViewModeChanged = { newMode ->
                onEvent(GalleryEvent.ChangeViewMode(newMode))
            },
            personAnimatedVisibilityScope = galleryToDetailScope,
            tabAnimatedVisibilityScope = tabAnimatedVisibilityScope
        )
    }
}