package com.myurrrjj.chronicle.data.repository

import com.myurrrjj.chronicle.data.local.dao.FaceDao
import com.myurrrjj.chronicle.domain.model.PersonCluster
import com.myurrrjj.chronicle.domain.repository.FaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FaceRepositoryImpl @Inject constructor(
    private val faceDao: FaceDao
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
                    mediaCount = summary.mediaCount
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getClusterById(clusterId: Long): PersonCluster? = withContext(Dispatchers.IO) {
        val entity = faceDao.getClusterById(clusterId) ?: return@withContext null
        val coverEmbedding = faceDao.getCoverEmbeddingForCluster(clusterId)
        val mediaCount =
            faceDao.getMediaCountForCluster(clusterId)

        PersonCluster(
            id = entity.clusterId,
            name = entity.personLabel,

            coverMediaId = coverEmbedding?.mediaId,
            coverEmbeddingId = coverEmbedding?.embeddingId,
                    mediaCount = mediaCount

        )
    }

    override suspend fun updateClusterName(clusterId: Long, name: String) = withContext(Dispatchers.IO) {
        faceDao.updateClusterLabel(clusterId = clusterId, newLabel = name)
    }

    override suspend fun getMediaIdsForCluster(clusterId: Long): List<Long> = withContext(Dispatchers.IO) {
        faceDao.getMediaIdsForCluster(clusterId)
    }

    override suspend fun deleteCluster(clusterId: Long) = withContext(Dispatchers.IO) {
        faceDao.deleteCluster(clusterId)
    }
}