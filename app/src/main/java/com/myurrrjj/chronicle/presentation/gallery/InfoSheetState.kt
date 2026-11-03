package com.myurrrjj.chronicle.presentation.gallery

import com.myurrrjj.chronicle.domain.model.DetailedMediaInfo
import com.myurrrjj.chronicle.domain.model.MediaAsset

sealed interface InfoSheetState {
    object Closed : InfoSheetState
    data class Loading(val media: MediaAsset) : InfoSheetState
    data class Success(val media: MediaAsset, val details: DetailedMediaInfo) : InfoSheetState
    data class Error(val media: MediaAsset, val message: String) : InfoSheetState
}