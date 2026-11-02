package com.example.chronicle.domain.repository

import com.example.chronicle.domain.model.AppSettings
import com.example.chronicle.domain.model.GalleryViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings : Flow<AppSettings>
    suspend fun updateAutoPlayVideos(enabled:Boolean)
    suspend fun updateDefaultGalleryViewMode(viewMode: GalleryViewMode)


}