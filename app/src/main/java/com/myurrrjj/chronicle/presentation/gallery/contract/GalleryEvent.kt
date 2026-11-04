package com.myurrrjj.chronicle.presentation.gallery.contract

import com.myurrrjj.chronicle.core.presentation.mvi.ViewEvent
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.GalleryViewMode
import com.myurrrjj.chronicle.domain.model.MediaAsset
import com.myurrrjj.chronicle.domain.model.PersonCluster
import com.myurrrjj.chronicle.presentation.gallery.components.DetailAction
import com.myurrrjj.chronicle.presentation.gallery.model.Album
import com.myurrrjj.chronicle.presentation.gallery.model.GalleryTab
import com.myurrrjj.chronicle.presentation.gallery.model.OthersSelection

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


    object OpenFaceIndexingSheet : GalleryEvent()

    object DismissFaceIndexingSheet : GalleryEvent()

    data class StartFaceIndexing(val options: FaceIndexingOptions) : GalleryEvent()

    object PauseFaceIndexing : GalleryEvent()

    object ResumeFaceIndexing : GalleryEvent()

    object ForceStopFaceIndexing : GalleryEvent()

    object ResetFaceIndexing : GalleryEvent()
    object OnSettingsClicked : GalleryEvent()

    object OnSearchClicked : GalleryEvent()

    data class SetSearchMediaList(val results: List<MediaAsset>) : GalleryEvent()
    object ClearSearchMediaList : GalleryEvent()

}
