package com.myurrrjj.chronicle.domain.repository

import com.myurrrjj.chronicle.domain.model.DetailedMediaInfo
import com.myurrrjj.chronicle.domain.model.MediaAsset
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaAsset>>

    fun getFavorites(): Flow<List<MediaAsset>>

    suspend fun toggleFavourite(media: MediaAsset, isFavourite: Boolean)

    suspend fun getMediaDetails(uriString: String, isVideo: Boolean): DetailedMediaInfo

}