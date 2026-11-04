package com.myurrrjj.chronicle.domain.repository

import com.myurrrjj.chronicle.domain.model.AppSettings
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.GalleryViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings : Flow<AppSettings>
    suspend fun updateAutoPlayVideos(enabled:Boolean)
    suspend fun updateDefaultGalleryViewMode(viewMode: GalleryViewMode)

    suspend fun setFaceIndexingPaused(paused: Boolean)

    suspend fun updateFaceIndexingOptions(options: FaceIndexingOptions)




}