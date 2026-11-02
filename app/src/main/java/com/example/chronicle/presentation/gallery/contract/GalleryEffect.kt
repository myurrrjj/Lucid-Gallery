package com.example.chronicle.presentation.gallery.contract

import com.example.chronicle.core.presentation.mvi.ViewSideEffect

sealed class GalleryEffect : ViewSideEffect {
    object RequestPermission : GalleryEffect()
    data class NavigateToDetail(val mediaId: Long) : GalleryEffect()

    data class ShareMedia(val uris: List<String>, val mimeType: String) : GalleryEffect()
    data class DeleteMedia(val uris: List<String>) : GalleryEffect()
    data class EditMedia(val uriString: String, val mimeType: String) : GalleryEffect()

    data class CopyMedia(val uris: List<String>) : GalleryEffect()
    object NavigateToSettings : GalleryEffect()
    object NavigateToSearch : GalleryEffect()
}
