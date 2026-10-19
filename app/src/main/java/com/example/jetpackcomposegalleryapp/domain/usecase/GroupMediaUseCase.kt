package com.example.jetpackcomposegalleryapp.domain.usecase

import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import javax.inject.Inject

class GroupMediaUseCase @Inject constructor() {
    operator fun invoke(
        mediaList: List<MediaAsset>,
        viewMode: GalleryViewMode
    ): ImmutableMap<String, ImmutableList<MediaAsset>> {
        return mediaList
            .groupBy {viewMode.getGroupingKey(it.dateAdded)}
            .mapValues { (_,items)->items.toImmutableList() }
            .toImmutableMap()
    }
}