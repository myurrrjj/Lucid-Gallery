package com.example.jetpackcomposegalleryapp.worker

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
import com.example.jetpackcomposegalleryapp.core.ml.FaceClusterer
import com.example.jetpackcomposegalleryapp.core.ml.FaceCropper
import com.example.jetpackcomposegalleryapp.core.ml.FaceEmbedder
import com.example.jetpackcomposegalleryapp.core.ml.MlKitFaceDetector
import com.example.jetpackcomposegalleryapp.core.util.BitmapLoader
import com.example.jetpackcomposegalleryapp.data.local.dao.FaceDao
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceEmbeddingEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaProcessingStatusEntity
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class FaceIndexingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val faceDao: FaceDao,
    private val faceDetector: MlKitFaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val faceClusterer: FaceClusterer,
    private val contentResolver: ContentResolver
) : CoroutineWorker(context, params) {


    private val notificationId = 1001
    private val channelId = "face_indexing_channel"
    private val BATCH_SIZE = 20

    companion object {
        private const val TAG = "FaceIndexingWorker"
        private const val MAX_ATTEMPTS = 3
    }

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Face Indexing", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Gallery: Finding People")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        createForegroundInfo("Starting face indexing...")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            setForeground(getForegroundInfo())

            val allMedia = mediaRepository.getAllMedia().first()
            val alreadyProcessedIds = faceDao.getProcessedMediaIds().toSet()
            val pendingMedia = allMedia.filter { it.id !in alreadyProcessedIds && !it.isVideo }

            if (pendingMedia.isEmpty()) return@withContext Result.success()

            val total = pendingMedia.size
            val chunkedMedia = pendingMedia.chunked(BATCH_SIZE)
            var processedCount = 0

            for (chunk in chunkedMedia) {
                ensureActive()


                val batchResults = mutableListOf<Pair<FaceEmbeddingEntity, Bitmap>>()

                val processingLedger = mutableListOf<MediaProcessingStatusEntity>()


                for (media in chunk) {
                    ensureActive()
                    val facesForMedia = processSingleMedia(media)
                    batchResults.addAll(facesForMedia)

                    processingLedger.add(
                        MediaProcessingStatusEntity(
                            mediaId = media.id,
                            facesFound = facesForMedia.size

                        )
                    )
                    processedCount++
                }

                try {
                    ensureActive()
                    faceDao.insertMediaProcessingStatuses(processingLedger)

                    if (batchResults.isNotEmpty()) {
                        val entities = batchResults.map { it.first }
                        val insertedIds = faceDao.insertEmbeddings(entities)
                        val cropDir = File(context.filesDir, "face_crops").apply { mkdirs() }

                        val savedEmbeddings = entities.mapIndexed { index, entity ->
                            val id = insertedIds[index]
                            val bitmapToSave = batchResults[index].second

                            val file = File(cropDir, "$id.jpg")
                            FileOutputStream(file).use { out ->
                                bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }

                            bitmapToSave.recycle()
                            entity.copy(embeddingId = id)
                        }

                        ensureActive()
                        val existingClusters = getActiveClustersMap()
                        faceClusterer.clusterEmbeddings(savedEmbeddings, existingClusters)
                    }
                } finally {
                    batchResults.forEach { it.second.recycle() }
                }
                setForeground(createForegroundInfo("Processed $processedCount of $total images"))
            }

            Result.success()
        } catch (e: CancellationException) {
            Log.i(TAG, "Worker cancelled (e.g., device unplugged)")
            throw e
        } catch (e: OutOfMemoryError) {
            System.gc()
            Log.e(TAG, "OOM on attempt $runAttemptCount — stopping permanently.", e)
            Result.failure()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed on attempt $runAttemptCount (${e::class.simpleName})", e)
            if (runAttemptCount >= MAX_ATTEMPTS - 1) Result.failure() else Result.retry()
        }
    }

    private suspend fun processSingleMedia(media: MediaAsset): List<Pair<FaceEmbeddingEntity, Bitmap>> {
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

                        val boundingBox = face.boundingBox ?: continue
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
                        preProcessed.recycle()
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

    private suspend fun getActiveClustersMap(): Map<Long, List<FaceEmbeddingEntity>> {
        val activeClustersMap = mutableMapOf<Long, List<FaceEmbeddingEntity>>()
        val allClusters = faceDao.getAllClusters().first()

        for (cluster in allClusters) {
            val embeddingsForCluster = faceDao.getEmbeddingsForClusterId(cluster.clusterId)
            activeClustersMap[cluster.clusterId] = embeddingsForCluster
        }
        return activeClustersMap
    }
}