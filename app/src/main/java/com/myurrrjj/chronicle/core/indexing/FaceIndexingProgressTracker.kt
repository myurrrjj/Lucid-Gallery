package com.myurrrjj.chronicle.core.indexing

import com.myurrrjj.chronicle.domain.model.FaceIndexingProgress
import com.myurrrjj.chronicle.domain.model.FaceIndexingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceIndexingProgressTracker @Inject constructor() {

    private val _state = MutableStateFlow(FaceIndexingProgress())
    val state = _state.asStateFlow()
    fun begin(total: Int, processed: Int, facesFound: Int) {
        _state.value = FaceIndexingProgress(
            status = FaceIndexingStatus.RUNNING,
            processed = processed,
            total = total,
            facesFound = facesFound,
            currentItem = null,
            message = null
        )
    }

    fun report(
        processed: Int, total: Int, facesFound: Int, currentItem: String?
    ) {
        _state.update {
            it.copy(
                status = FaceIndexingStatus.RUNNING,
                processed = processed,
                total = total,
                facesFound = facesFound,
                currentItem = currentItem,
                message = null
            )
        }
    }

    fun requestPause() {
        _state.update {
            it.copy(
                status = FaceIndexingStatus.PAUSING, message = "Finishing the current batch…"
            )
        }
    }

    fun pauseConfirmed(processed: Int, total: Int) {
        _state.update {
            it.copy(
                status = FaceIndexingStatus.PAUSED,
                processed = processed,
                total = total,
                currentItem = null,
                message = null
            )
        }
    }


    fun setStatus(status: FaceIndexingStatus, message: String? = null) {
        _state.update { it.copy(status = status, message = message) }
    }

    fun complete(total: Int) {
        _state.update {
            it.copy(
                status = FaceIndexingStatus.COMPLETED,
                processed = total,
                total = total,
                currentItem = null,
                message = null
            )
        }
    }

    fun fail(message: String) {
        _state.update {
            it.copy(
                status = FaceIndexingStatus.FAILED, currentItem = null, message = message
            )
        }
    }

    fun reset() {
        _state.value = FaceIndexingProgress()
    }


}