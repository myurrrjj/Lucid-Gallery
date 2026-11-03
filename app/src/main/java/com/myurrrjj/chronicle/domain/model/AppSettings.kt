package com.myurrrjj.chronicle.domain.model


data class AppSettings(
    val autoPlayVideo: Boolean = true,
    val defaultGalleryViewMode: GalleryViewMode = GalleryViewMode.MONTH
)