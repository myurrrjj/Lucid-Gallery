package com.example.jetpackcomposegalleryapp.presentation.gallery.contract

import com.example.jetpackcomposegalleryapp.core.presentation.mvi.ViewEvent
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.Album
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.OthersSelection

sealed class GalleryEvent : ViewEvent {
    object LoadMedia : GalleryEvent()
    data class PermissionResult(val isGranted: Boolean) : GalleryEvent()
    data class MediaClicked(val mediaId: Long) : GalleryEvent()
    data class OnTabSelected(val tab: GalleryTab) : GalleryEvent()
    data class OnOthersSelectionChanged(val selection: OthersSelection) : GalleryEvent()
    data class OpenPerson(val cluster: PersonCluster) : GalleryEvent()
    object ClosePerson : GalleryEvent()
    data class UpdatePersonName(val clusterId: Long, val newName: String) : GalleryEvent()
    data class PerformMediaAction(
        val action: DetailAction,
        val mediaList: List<MediaAsset> = emptyList()
    ) : GalleryEvent()

    data class OpenInfoSheet(val media: MediaAsset) : GalleryEvent()
    object CloseInfoSheet : GalleryEvent()

    data class OpenAlbum(val album: Album) : GalleryEvent()
    object CloseAlbum : GalleryEvent()

    object EnterSelectionMode : GalleryEvent()
    object ExitSelectionMode : GalleryEvent()
    data class ToggleMediaSelection(val mediaId: Long) : GalleryEvent()
    object SelectAll : GalleryEvent()
    object ClearSelection : GalleryEvent()
    data class ChangeViewMode(val viewMode: GalleryViewMode) : GalleryEvent()

    object StartFaceIndexing : GalleryEvent()
    object OnSettingsClicked : GalleryEvent()

    object onSearchClicked : GalleryEvent()

    data class SetSearchMediaList(val results: List<MediaAsset>) : GalleryEvent()
    object ClearSearchMediaList : GalleryEvent()

}
