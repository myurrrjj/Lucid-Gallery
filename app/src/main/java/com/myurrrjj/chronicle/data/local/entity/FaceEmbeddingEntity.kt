package com.myurrrjj.chronicle.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.myurrrjj.chronicle.data.local.converters.Converters

@Entity(tableName = "face_embeddings", indices  =[Index("mediaId")])
@TypeConverters(Converters::class)
data class FaceEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    val embeddingId: Long = 0,
    val mediaId: Long,
    val faceIndex: Int,
    val embedding: FloatArray,
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float,
    val processedAt:Long = System.currentTimeMillis()

    )
{
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEmbeddingEntity

        if (embeddingId != other.embeddingId) return false
        if (mediaId != other.mediaId) return false
        if (faceIndex != other.faceIndex) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (boundsLeft != other.boundsLeft) return false
        if (boundsTop != other.boundsTop) return false
        if (boundsRight != other.boundsRight) return false
        if (boundsBottom != other.boundsBottom) return false
        if (processedAt != other.processedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = embeddingId.hashCode()
        result = 31 * result + mediaId.hashCode()
        result = 31 * result + faceIndex
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + boundsLeft.hashCode()
        result = 31 * result + boundsTop.hashCode()
        result = 31 * result + boundsRight.hashCode()
        result = 31 * result + boundsBottom.hashCode()
        result = 31 * result + processedAt.hashCode()
        return result    }
}
