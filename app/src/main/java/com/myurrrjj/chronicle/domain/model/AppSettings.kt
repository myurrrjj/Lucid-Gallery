package com.myurrrjj.chronicle.domain.model

import com.myurrrjj.chronicle.presentation.settings.SettingItem


data class AppSettings(
    val autoPlayVideo: Boolean = true,
    val defaultGalleryViewMode: GalleryViewMode = GalleryViewMode.DAY,
    val faceIndexingPaused:Boolean = false,
    val faceIndexingOptions: FaceIndexingOptions = FaceIndexingOptions()


)