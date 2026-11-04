package com.myurrrjj.chronicle.data.repository

import android.content.Context
import com.myurrrjj.chronicle.data.local.dao.FaceDao
import com.myurrrjj.chronicle.domain.model.PersonCluster
import com.myurrrjj.chronicle.domain.repository.FaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class FaceRepositoryImpl @Inject constructor(
    private val faceDao: FaceDao, @ApplicationContext private val context: Context

) : FaceRepository {

    override fun getAllClusters(): Flow<List<PersonCluster>> {
        return faceDao.observeClusterSummaries().map { summaries ->
            summaries.map { summary ->
//                val coverEmbedding = faceDao.getCoverEmbeddingForCluster(entity.clusterId)
                PersonCluster(
                    id = summary.cluster.clusterId,
                    name = summary.cluster.personLabel,
                    coverMediaId = summary.coverMediaId,
                    coverEmbeddingId = summary.cluster.coverEmbeddingId,
                    mediaCount = summary.mediaCount,
                    coverFaceIndex = summary.coverFaceIndex,

                    )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getClusterById(clusterId: Long): PersonCluster? =
        withContext(Dispatchers.IO) {
            val entity = faceDao.getClusterById(clusterId) ?: return@withContext null
            val coverEmbedding = faceDao.getCoverEmbeddingForCluster(clusterId)
            val mediaCount = faceDao.getMediaCountForCluster(clusterId)

            PersonCluster(
                id = entity.clusterId,
                name = entity.personLabel,

                coverMediaId = coverEmbedding?.mediaId,
                coverEmbeddingId = coverEmbedding?.embeddingId,
                mediaCount = mediaCount,
                coverFaceIndex = coverEmbedding?.faceIndex,


                )
        }

    override suspend fun updateClusterName(clusterId: Long, name: String) =
        withContext(Dispatchers.IO) {
            faceDao.updateClusterLabel(clusterId = clusterId, newLabel = name)
        }

    override suspend fun getMediaIdsForCluster(clusterId: Long): List<Long> =
        withContext(Dispatchers.IO) {
            faceDao.getMediaIdsForCluster(clusterId)
        }

    override suspend fun deleteCluster(clusterId: Long) = withContext(Dispatchers.IO) {
        faceDao.deleteCluster(clusterId)
    }

    override fun observeProcessedCount(): Flow<Int> =
        faceDao.observeProcessedCount().flowOn(Dispatchers.IO)


    override fun observeFaceCount(): Flow<Int> = faceDao.observeFaceCount().flowOn(Dispatchers.IO)


    override suspend fun resetFaceIndexing(): Unit = withContext(Dispatchers.IO) {
        faceDao.deleteAllFaceData()
        val cropDir = File(context.filesDir, "face_crops")
        cropDir.listFiles()?.forEach { file ->
            runCatching { file.delete() }
        }

    }
}