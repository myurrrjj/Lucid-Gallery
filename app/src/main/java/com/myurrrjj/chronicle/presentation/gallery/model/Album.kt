package com.myurrrjj.chronicle.presentation.gallery.model

import com.myurrrjj.chronicle.domain.model.MediaAsset

data class Album(
    val name: String?,
    val mediaCount: Int,
    val coverMedia: MediaAsset
)
