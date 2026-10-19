package com.example.jetpackcomposegalleryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceClusterEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.FaceEmbeddingEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaPersonEntity
import com.example.jetpackcomposegalleryapp.data.local.entity.MediaProcessingStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: FaceEmbeddingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(embeddings: List<FaceEmbeddingEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaProcessingStatuses(statuses: List<MediaProcessingStatusEntity>)

    @Query("select * from face_embeddings where mediaId = :mediaId order by faceIndex ASC")
    suspend fun getEmbeddingsForMedia(mediaId: Long): List<FaceEmbeddingEntity>

    @Query("SELECT * FROM face_embeddings WHERE embeddingId = :embeddingId")
    suspend fun getEmbeddingById(embeddingId: Long): FaceEmbeddingEntity?

    @Query("SELECT mediaId FROM media_processing_status")
    suspend fun getProcessedMediaIds(): List<Long>

    @Query("SELECT COUNT(*) FROM face_embeddings WHERE mediaId = :mediaId")
    suspend fun getFaceCountForMedia(mediaId: Long): Int

    @Query("DELETE FROM face_embeddings WHERE mediaId = :mediaId")
    suspend fun deleteEmbeddingsForMedia(mediaId: Long)

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAllEmbeddings(): List<FaceEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: FaceClusterEntity): Long

    @Query("SELECT * FROM face_clusters ORDER BY personLabel COLLATE NOCASE ASC")
    fun getAllClusters(): Flow<List<FaceClusterEntity>>

    @Query("SELECT * FROM face_clusters WHERE clusterId = :clusterId")
    suspend fun getClusterById(clusterId: Long): FaceClusterEntity?

    @Query("UPDATE face_clusters SET personLabel = :newLabel, lastUpdatedAt = :updatedAt WHERE clusterId = :clusterId")
    suspend fun updateClusterLabel(
        clusterId: Long,
        newLabel: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM face_clusters WHERE clusterId = :clusterId")
    suspend fun deleteCluster(clusterId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaPerson(mediaPerson: MediaPersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaPersons(mediaPersons: List<MediaPersonEntity>)

    @Query("SELECT embeddingId FROM media_person WHERE clusterId = :clusterId")
    suspend fun getEmbeddingIdsForCluster(clusterId: Long): List<Long>

    @Query(
        """
        SELECT DISTINCT e.mediaId 
        FROM media_person mp
        JOIN face_embeddings e ON mp.embeddingId = e.embeddingId
        WHERE mp.clusterId = :clusterId
    """
    )
    suspend fun getMediaIdsForCluster(clusterId: Long): List<Long>

    @Query("""
    SELECT e.* FROM face_embeddings e
    JOIN media_person mp ON e.embeddingId = mp.embeddingId
    WHERE mp.clusterId = :clusterId
""")
    suspend fun getEmbeddingsForClusterId(clusterId: Long): List<FaceEmbeddingEntity>

    @Query(
        """
        SELECT DISTINCT mp.clusterId
        FROM media_person mp
        JOIN face_embeddings e ON mp.embeddingId = e.embeddingId
        WHERE e.mediaId = :mediaId
    """
    )
    suspend fun getClusterIdsForMedia(mediaId: Long): List<Long>

    @Query("DELETE FROM media_person WHERE embeddingId = :embeddingId AND clusterId = :clusterId")
    suspend fun deleteMediaPerson(embeddingId: Long, clusterId: Long)

    @Query(
        """
        SELECT COUNT(DISTINCT e.mediaId)
        FROM media_person mp
        JOIN face_embeddings e ON mp.embeddingId = e.embeddingId
        WHERE mp.clusterId = :clusterId
    """
    )
    suspend fun getMediaCountForCluster(clusterId: Long): Int

    @Query(
        """
        SELECT e.* FROM face_embeddings e
        JOIN media_person mp ON e.embeddingId = mp.embeddingId
        WHERE mp.clusterId = :clusterId
        LIMIT 1
    """
    )
    suspend fun getCoverEmbeddingForCluster(clusterId: Long): FaceEmbeddingEntity?

    @Transaction
    suspend fun deleteAllFaceData() {
        deleteAllEmbeddings()
        deleteAllClusters()
        deleteAllProcessingStatuses()
    }

    @Query("DELETE FROM media_processing_status")
    suspend fun deleteAllProcessingStatuses()

    @Query("DELETE FROM face_embeddings")
    suspend fun deleteAllEmbeddings()

    @Query("DELETE FROM face_clusters")
    suspend fun deleteAllClusters()
}