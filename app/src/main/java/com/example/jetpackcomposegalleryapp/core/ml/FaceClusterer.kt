package com.example.jetpackcomposegalleryapp.core.ml

import com.example.jetpackcomposegalleryapp.data.local.dao.FaceDao
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceClusterEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceEmbeddingEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaPersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.sqrt

class FaceClusterer @Inject constructor(private val faceDao: FaceDao) {

    companion object {
        private const val MATCH_THRESHOLD = 0.36f
    }

    suspend fun clusterEmbeddings(
        unbound: List<FaceEmbeddingEntity>,
        clusters: Map<Long, List<FaceEmbeddingEntity>>
    ) = withContext(Dispatchers.Default) {

        if (unbound.isEmpty()) return@withContext

        val activeClusters = clusters.mapValues { it.value.toMutableList() }.toMutableMap()
        val mediaPersonsToInsert = mutableListOf<MediaPersonEntity>()

        for (newFace in unbound) {
            ensureActive()
            var bestClusterId: Long? = null
            var highestSimilarity = -1f

            for ((clusterId, existingFaces) in activeClusters) {
                for (existingFace in existingFaces) {
                    val sim = cosineSimilarity(newFace.embedding, existingFace.embedding)

                    if (sim > highestSimilarity) {
                        highestSimilarity = sim
                        if (sim >= MATCH_THRESHOLD) {
                            bestClusterId = clusterId
                        }
                    }
                }
            }

            if (bestClusterId != null) {
                mediaPersonsToInsert.add(MediaPersonEntity(embeddingId = newFace.embeddingId, clusterId = bestClusterId))
                activeClusters[bestClusterId]?.add(newFace)
            } else {
                val newCluster = FaceClusterEntity(
                    personLabel = "Person",
                    coverEmbeddingId = newFace.embeddingId
                )
                val newId = faceDao.insertCluster(newCluster)
                mediaPersonsToInsert.add(MediaPersonEntity(embeddingId = newFace.embeddingId, clusterId = newId))
                activeClusters[newId] = mutableListOf(newFace)
            }
        }

        if (mediaPersonsToInsert.isNotEmpty()) {
            faceDao.insertMediaPersons(mediaPersonsToInsert)
        }
    }

    private fun cosineSimilarity(vA: FloatArray, vB: FloatArray): Float {
        var dot = 0f; var nA = 0f; var nB = 0f
        for (i in vA.indices) {
            dot += vA[i] * vB[i]
            nA += vA[i] * vA[i]
            nB += vB[i] * vB[i]
        }
        return if (nA == 0f || nB == 0f) 0f else dot / (sqrt(nA) * sqrt(nB))
    }
}