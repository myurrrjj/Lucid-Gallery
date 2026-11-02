package com.example.chronicle.presentation.gallery.model

import com.example.chronicle.domain.model.MediaAsset

data class Album(
    val name: String?,
    val mediaCount: Int,
    val coverMedia: MediaAsset
)
