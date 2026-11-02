package com.example.chronicle.presentation.gallery

import com.example.chronicle.domain.model.DetailedMediaInfo
import com.example.chronicle.domain.model.MediaAsset

sealed interface InfoSheetState {
    object Closed : InfoSheetState
    data class Loading(val media: MediaAsset) : InfoSheetState
    data class Success(val media: MediaAsset, val details: DetailedMediaInfo) : InfoSheetState
    data class Error(val media: MediaAsset, val message: String) : InfoSheetState
}