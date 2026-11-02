package com.example.chronicle.domain.usecase

import com.example.chronicle.domain.model.MediaAsset
import com.example.chronicle.presentation.gallery.model.GalleryTab
import com.example.chronicle.presentation.gallery.model.OthersSelection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

class GetFilteredMediaUseCase @Inject constructor() {
    operator fun invoke(
        masterList: List<MediaAsset>,
        favouriteList: List<MediaAsset>,
        tab: GalleryTab,
        othersSelection: OthersSelection
    ): ImmutableList<MediaAsset> {
        return when (tab) {
            GalleryTab.ALL -> masterList.toImmutableList()
            GalleryTab.VIDEOS -> masterList.filter { it.isVideo }.toImmutableList()
            GalleryTab.ALBUMS -> persistentListOf()
            GalleryTab.OTHERS -> {
                if (othersSelection == OthersSelection.FAVOURITES) favouriteList.toImmutableList()
                else persistentListOf()
            }
        }
    }
}