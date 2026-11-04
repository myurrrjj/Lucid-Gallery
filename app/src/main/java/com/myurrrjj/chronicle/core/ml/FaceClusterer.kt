package com.myurrrjj.chronicle.core.ml

import com.myurrrjj.chronicle.data.local.dao.FaceDao
import com.myurrrjj.chronicle.data.local.entity.FaceClusterEntity
import com.myurrrjj.chronicle.data.local.entity.FaceEmbeddingEntity
import com.myurrrjj.chronicle.data.local.entity.MediaPersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.sqrt

class FaceClusterer @Inject constructor(private val faceDao: FaceDao) {

    companion object {
        private const val MATCH_THRESHOLD = 0.363f
    }

    suspend fun clusterEmbeddings(
        unbound: List<FaceEmbeddingEntity>,
//        clusters: Map<Long, List<FaceEmbeddingEntity>>
    ) = withContext(Dispatchers.Default) {

        if (unbound.isEmpty()) return@withContext
        val clusters = loadClusterMap()
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
                mediaPersonsToInsert.add(
                    MediaPersonEntity(embeddingId = newFace.embeddingId, clusterId = bestClusterId)

                )
                activeClusters[bestClusterId]?.add(newFace)

            } else {
                val newCluster = FaceClusterEntity(
                    personLabel = "Person ${activeClusters.size + 1}",
                    coverEmbeddingId = newFace.embeddingId
                )
                val newId = faceDao.insertCluster(newCluster)
                mediaPersonsToInsert.add(
                    MediaPersonEntity(
                        embeddingId = newFace.embeddingId, clusterId = newId
                    )
                )
                activeClusters[newId] = mutableListOf(newFace)

            }
        }
        if (mediaPersonsToInsert.isNotEmpty()) {
            faceDao.insertMediaPersons(mediaPersonsToInsert)
        }
    }


    suspend fun reclusterOrphans() = withContext(Dispatchers.Default) {
        val orphans = faceDao.getUnclusteredEmbeddings()
        if (orphans.isEmpty()) return@withContext
        clusterEmbeddings(orphans)
    }

    suspend fun loadClusterMap(): Map<Long, List<FaceEmbeddingEntity>> {
        val map = mutableMapOf<Long, List<FaceEmbeddingEntity>>()
        val allClusters = faceDao.getAllClusters().first()
        for (cluster in allClusters) {
            map[cluster.clusterId] = faceDao.getEmbeddingsForClusterId(cluster.clusterId)
        }
        return map
    }


    private fun cosineSimilarity(vA: FloatArray, vB: FloatArray): Float {
        var dot = 0f
        var nA = 0f
        var nB = 0f
        for (i in vA.indices) {
            dot += vA[i] * vB[i]
            nA += vA[i] * vA[i]
            nB += vB[i] * vB[i]
        }
        return if (nA == 0f || nB == 0f) 0f else dot / (sqrt(nA) * sqrt(nB))
    }
}