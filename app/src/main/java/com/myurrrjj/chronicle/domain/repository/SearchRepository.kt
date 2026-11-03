package com.myurrrjj.chronicle.domain.repository

import com.myurrrjj.chronicle.domain.model.MediaAsset
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun syncSearchIndex(mediaList: List<MediaAsset>, favouriteIds:Set<Long>): Flow<Float>
    suspend fun searchMediaIds(query: String): List<Long>
}