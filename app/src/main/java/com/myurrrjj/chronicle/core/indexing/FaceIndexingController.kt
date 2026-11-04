package com.myurrrjj.chronicle.core.indexing


import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.FaceIndexingProgress
import com.myurrrjj.chronicle.domain.model.FaceIndexingSchedule
import com.myurrrjj.chronicle.domain.model.FaceIndexingStatus
import com.myurrrjj.chronicle.domain.repository.FaceRepository
import com.myurrrjj.chronicle.domain.repository.MediaRepository
import com.myurrrjj.chronicle.domain.repository.SettingsRepository
import com.myurrrjj.chronicle.worker.FaceIndexingWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceIndexingController @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
    private val faceRepository: FaceRepository,
    private val mediaRepository: MediaRepository,
    private val tracker: FaceIndexingProgressTracker
) {

    companion object {
        const val UNIQUE_WORK_NAME = "FaceIndexing"

        const val KEY_TOTAL = "total"
        const val KEY_PROCESSED = "processed"
        const val KEY_FACES = "faces"
        const val KEY_OUTCOME = "outcome"

        const val OUTCOME_PAUSED = "paused"
        const val OUTCOME_COMPLETED = "completed"
        const val OUTCOME_FAILED = "failed"
    }

    private data class Snapshot(
        val tracked: FaceIndexingProgress,
        val paused: Boolean,
        val options: FaceIndexingOptions,
        val processed: Int,
        val faces: Int,
        val total: Int
    )


    val progress: Flow<FaceIndexingProgress> =
        combine(
            tracker.state,
            settingsRepository.settings,
            faceRepository.observeProcessedCount(),
            faceRepository.observeFaceCount(),
            mediaRepository.observeImageCount()
        ) { tracked, settings, processed, faces, total ->
            Snapshot(
                tracked = tracked,
                paused = settings.faceIndexingPaused,
                options = settings.faceIndexingOptions,
                processed = processed,
                faces = faces,
                total = total
            )
        }.combine(workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)) { snapshot, infos ->
            snapshot.toProgress(infos.firstOrNull())
        }


    suspend fun start(options: FaceIndexingOptions) {
        settingsRepository.updateFaceIndexingOptions(options)
        settingsRepository.setFaceIndexingPaused(false)
        tracker.setStatus(FaceIndexingStatus.QUEUED, "Preparing…")
        enqueue(options)
    }


    suspend fun resume() {
        val options = settingsRepository.settings.first().faceIndexingOptions
        settingsRepository.setFaceIndexingPaused(false)
        tracker.setStatus(FaceIndexingStatus.QUEUED, "Resuming…")
        enqueue(options)
    }

    suspend fun pause() {
        settingsRepository.setFaceIndexingPaused(true)
        tracker.requestPause()
    }


    suspend fun forceStop() {
        settingsRepository.setFaceIndexingPaused(true)
        tracker.setStatus(FaceIndexingStatus.PAUSED, "Stopped")
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    suspend fun reset() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        settingsRepository.setFaceIndexingPaused(false)
        faceRepository.resetFaceIndexing()
        tracker.reset()
    }

    suspend fun updateOptions(options: FaceIndexingOptions) {
        settingsRepository.updateFaceIndexingOptions(options)
    }


    suspend fun autoStartIfNeeded() {
        val settings = settingsRepository.settings.first()
        if (settings.faceIndexingPaused) return
        if (settings.faceIndexingOptions.schedule != FaceIndexingSchedule.ON_APP_OPEN) return
        if (hasRunningWork()) return

        val current = progress.first()
        if (current.pending <= 0) return

        tracker.setStatus(FaceIndexingStatus.QUEUED, "Resuming…")
        enqueue(settings.faceIndexingOptions)
    }


    private suspend fun enqueue(options: FaceIndexingOptions) {
        if (hasRunningWork()) return

        val request = OneTimeWorkRequestBuilder<FaceIndexingWorker>()
            .setConstraints(buildConstraints(options))
            .addTag(UNIQUE_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun buildConstraints(options: FaceIndexingOptions): Constraints {
        val needsCharging = options.schedule == FaceIndexingSchedule.WHILE_CHARGING ||
                options.schedule == FaceIndexingSchedule.WHEN_IDLE
        val needsIdle = options.schedule == FaceIndexingSchedule.WHEN_IDLE

        return Constraints.Builder()
            .setRequiresBatteryNotLow(options.pauseOnLowBattery)
            .setRequiresStorageNotLow(options.pauseOnLowStorage)
            .setRequiresCharging(needsCharging)
            .setRequiresDeviceIdle(needsIdle)
            .build()
    }

    private suspend fun hasRunningWork(): Boolean =
        workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
            .first()
            .any { it.state == WorkInfo.State.RUNNING }



    private fun Snapshot.toProgress(info: WorkInfo?): FaceIndexingProgress {

        val workerRunning = info?.state == WorkInfo.State.RUNNING
        val workerQueued = info?.state == WorkInfo.State.ENQUEUED ||
                info?.state == WorkInfo.State.BLOCKED
        val workerFailed = info?.state == WorkInfo.State.FAILED

        val live = workerRunning &&
                (tracked.status == FaceIndexingStatus.RUNNING ||
                        tracked.status == FaceIndexingStatus.PAUSING)


        val effectiveProcessed = if (live) maxOf(tracked.processed, processed) else processed

        val effectiveTotal = if (total > 0) total else tracked.total

        val status = when {
            workerRunning && tracked.status == FaceIndexingStatus.PAUSING -> FaceIndexingStatus.PAUSING
            workerRunning -> FaceIndexingStatus.RUNNING

            workerFailed -> FaceIndexingStatus.FAILED

            workerQueued && !paused -> FaceIndexingStatus.QUEUED

            paused -> FaceIndexingStatus.PAUSED

            effectiveTotal in 1..effectiveProcessed -> FaceIndexingStatus.COMPLETED

            effectiveProcessed > 0 -> FaceIndexingStatus.PAUSED

            else -> FaceIndexingStatus.IDLE
        }

        return FaceIndexingProgress(
            status = status,
            processed = if (effectiveTotal > 0) {
                effectiveProcessed.coerceIn(0, effectiveTotal)
            } else {
                effectiveProcessed
            },
            total = effectiveTotal,
            facesFound = faces,
            currentItem = if (live) tracked.currentItem else null,
            message = when {
                status == FaceIndexingStatus.PAUSING -> "Finishing the current batch…"
                status == FaceIndexingStatus.QUEUED -> "Waiting for charging / idle"
                status == FaceIndexingStatus.FAILED ->
                    tracked.message ?: "Indexing stopped unexpectedly"
                live -> tracked.message
                else -> null
            }
        )
    }
}