package com.myurrrjj.chronicle.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myurrrjj.chronicle.core.indexing.FaceIndexingController
import com.myurrrjj.chronicle.core.indexing.FaceIndexingProgressTracker
import com.myurrrjj.chronicle.core.ml.FaceClusterer
import com.myurrrjj.chronicle.core.ml.FaceCropper
import com.myurrrjj.chronicle.core.ml.FaceEmbedder
import com.myurrrjj.chronicle.core.ml.MlKitFaceDetector
import com.myurrrjj.chronicle.core.util.BitmapLoader
import com.myurrrjj.chronicle.data.local.dao.FaceDao
import com.myurrrjj.chronicle.data.local.entity.FaceEmbeddingEntity
import com.myurrrjj.chronicle.data.local.entity.MediaProcessingStatusEntity
import com.myurrrjj.chronicle.domain.model.MediaAsset
import com.myurrrjj.chronicle.domain.repository.MediaRepository
import com.myurrrjj.chronicle.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class FaceIndexingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    private val faceDao: FaceDao,
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer,
    private val progressTracker: FaceIndexingProgressTracker,
    private val contentResolver: ContentResolver
) : CoroutineWorker(context, params) {

    private val notificationId = 1001
    private val channelId = "face_indexing_channel"
    private val BATCH_SIZE = 20

    companion object {
        private const val TAG = "FaceIndexingWorker"
        private const val MAX_ATTEMPTS = 3
        private const val CROP_DIR = "face_crops"
    }

    private fun createForegroundInfo(
        progressText: String, processed: Int, total: Int
    ): ForegroundInfo {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Face Indexing", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Gallery: Finding People").setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_menu_gallery).setOngoing(true)
            .setOnlyAlertOnce(true)

        if (total > 0) {
            builder.setProgress(total, processed, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        val notification = builder.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Starting face indexing…", 0, 0)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setForeground(createForegroundInfo("Preparing your photos…", 0, 0))

            if (isPauseRequested()) {
                progressTracker.pauseConfirmed(0, 0)
                return@withContext Result.success(
                    workDataOf(
                        FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_PAUSED
                    )
                )
            }

            faceClusterer.reclusterOrphans()

            val allImages = mediaRepository.getAllMedia().first().filter { !it.isVideo }
            val alreadyProcessedIds = faceDao.getProcessedMediaIds().toSet()
            val pendingMedia = allImages.filter { it.id !in alreadyProcessedIds }

            val totalLibrary = allImages.size
            var processedCount = totalLibrary - pendingMedia.size
            var facesCount = faceDao.getFaceCount()

            progressTracker.begin(
                total = totalLibrary, processed = processedCount, facesFound = facesCount
            )

            if (pendingMedia.isEmpty()) {
                progressTracker.complete(totalLibrary)
                return@withContext Result.success(
                    workDataOf(
                        FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_COMPLETED,
                        FaceIndexingController.KEY_PROCESSED to totalLibrary,
                        FaceIndexingController.KEY_TOTAL to totalLibrary,
                        FaceIndexingController.KEY_FACES to facesCount
                    )
                )
            }

            val cropDir = File(context.filesDir, CROP_DIR).apply { mkdirs() }
            var pausedOut = false

            for (chunk in pendingMedia.chunked(BATCH_SIZE)) {
                ensureActive()

                if (isPauseRequested()) {
                    yield()

                    if (isPauseRequested()) {
                        pausedOut = true
                        break
                    }
                }

                val batchEmbeddings = mutableListOf<Pair<FaceEmbeddingEntity, Bitmap>>()
                val ledger = mutableListOf<MediaProcessingStatusEntity>()

                for (media in chunk) {
                    ensureActive()

                    val facesForMedia = processSingleMedia(media)
                    batchEmbeddings.addAll(facesForMedia)

                    ledger.add(
                        MediaProcessingStatusEntity(
                            mediaId = media.id, facesFound = facesForMedia.size
                        )
                    )

                    processedCount++

                    progressTracker.report(
                        processed = processedCount,
                        total = totalLibrary,
                        facesFound = facesCount + batchEmbeddings.size,
                        currentItem = media.name
                    )
                }

                batchEmbeddings.forEach { (entity, bitmap) ->
                    writeCrop(cropDir, entity.mediaId, entity.faceIndex, bitmap)
                }

                chunk.forEach { media ->
                    val faceCount = batchEmbeddings.count { it.first.mediaId == media.id }
                    pruneExtraCrops(cropDir, media.id, faceCount)
                }

                val savedEntities = try {
                    val insertedIds = faceDao.commitProcessedBatch(
                        mediaIds = chunk.map { it.id },
                        embeddings = batchEmbeddings.map { it.first },
                        statuses = ledger
                    )

                    batchEmbeddings.mapIndexed { index, pair ->
                        pair.first.copy(embeddingId = insertedIds[index])
                    }
                } finally {
                    batchEmbeddings.forEach { it.second.recycle() }
                }

                ensureActive()
                faceClusterer.clusterEmbeddings(savedEntities)

                facesCount = faceDao.getFaceCount()

                progressTracker.report(
                    processed = processedCount,
                    total = totalLibrary,
                    facesFound = facesCount,
                    currentItem = null
                )

                setForeground(
                    createForegroundInfo(
                        "Indexed $processedCount of $totalLibrary photos",
                        processedCount,
                        totalLibrary
                    )
                )

                setProgress(
                    workDataOf(
                        FaceIndexingController.KEY_PROCESSED to processedCount,
                        FaceIndexingController.KEY_TOTAL to totalLibrary,
                        FaceIndexingController.KEY_FACES to facesCount
                    )
                )
            }

            facesCount = faceDao.getFaceCount()

            if (pausedOut) {
                progressTracker.pauseConfirmed(processedCount, totalLibrary)

                Result.success(
                    workDataOf(
                        FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_PAUSED,
                        FaceIndexingController.KEY_PROCESSED to processedCount,
                        FaceIndexingController.KEY_TOTAL to totalLibrary,
                        FaceIndexingController.KEY_FACES to facesCount
                    )
                )
            } else {
                progressTracker.complete(totalLibrary)

                Result.success(
                    workDataOf(
                        FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_COMPLETED,
                        FaceIndexingController.KEY_PROCESSED to totalLibrary,
                        FaceIndexingController.KEY_TOTAL to totalLibrary,
                        FaceIndexingController.KEY_FACES to facesCount
                    )
                )
            }
        } catch (e: CancellationException) {
            Log.i(TAG, "Worker cancelled (force stop, unplugged, or OS reclaim)")
            throw e
        } catch (e: OutOfMemoryError) {
            System.gc()
            Log.e(TAG, "OOM on attempt $runAttemptCount — stopping permanently.", e)

            progressTracker.fail("Ran out of memory. Resume to continue from here.")

            Result.failure(
                workDataOf(
                    FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_FAILED
                )
            )
        } catch (e: Exception) {
            Log.e(
                TAG, "Worker failed on attempt $runAttemptCount (${e::class.simpleName})", e
            )

            if (runAttemptCount >= MAX_ATTEMPTS - 1) {
                progressTracker.fail(e.message ?: "Indexing stopped unexpectedly.")

                Result.failure(
                    workDataOf(
                        FaceIndexingController.KEY_OUTCOME to FaceIndexingController.OUTCOME_FAILED
                    )
                )
            } else {
                Result.retry()
            }
        }
    }

    private suspend fun isPauseRequested(): Boolean = runCatching {
        settingsRepository.settings.first().faceIndexingPaused
    }.getOrDefault(false)

    private fun writeCrop(
        cropDir: File, mediaId: Long, faceIndex: Int, bitmap: Bitmap
    ) {
        val file = File(cropDir, "${mediaId}_${faceIndex}.jpg")

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write crop for media $mediaId face $faceIndex", e)
        }
    }

    private fun pruneExtraCrops(
        cropDir: File, mediaId: Long, keepCount: Int
    ) {
        val prefix = "${mediaId}_"

        cropDir.listFiles { file -> file.name.startsWith(prefix) }?.forEach { file ->
            val index = file.name.removePrefix(prefix).removeSuffix(".jpg").toIntOrNull()

            if (index == null || index >= keepCount) {
                runCatching { file.delete() }
            }
        }
    }

    private suspend fun processSingleMedia(
        media: MediaAsset
    ): List<Pair<FaceEmbeddingEntity, Bitmap>> {
        val bitmap = BitmapLoader.loadBitmap(contentResolver, media.uriString) ?: return emptyList()

        return try {
            val faces = faceDetector.detectFaces(bitmap)
            val results = mutableListOf<Pair<FaceEmbeddingEntity, Bitmap>>()

            for ((index, face) in faces.withIndex()) {
                coroutineContext.ensureActive()

                val croppedFace = FaceCropper.cropFace(bitmap, face, margin = 0.2f) ?: continue

                try {
                    val preProcessed = FaceCropper.preprocessForEmbedding(croppedFace)

                    try {
                        val embeddingArray = faceEmbedder.generateEmbedding(preProcessed)

                        if (embeddingArray == null) {
                            croppedFace.recycle()
                            continue
                        }

                        val boundingBox = face.boundingBox

                        val entity = FaceEmbeddingEntity(
                            mediaId = media.id,
                            faceIndex = index,
                            embedding = embeddingArray,
                            boundsLeft = boundingBox.left.toFloat(),
                            boundsTop = boundingBox.top.toFloat(),
                            boundsRight = boundingBox.right.toFloat(),
                            boundsBottom = boundingBox.bottom.toFloat(),
                            processedAt = System.currentTimeMillis()
                        )

                        results.add(Pair(entity, croppedFace))
                    } finally {
                        if (preProcessed !== croppedFace) {
                            preProcessed.recycle()
                        }
                    }
                } catch (e: Exception) {
                    croppedFace.recycle()

                    if (e is CancellationException) throw e

                    Log.e(TAG, "Error processing face in media ${media.id}", e)
                }
            }

            results
        } finally {
            bitmap.recycle()
        }
    }
}