package com.myurrrjj.chronicle.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_clusters",
    indices = [Index("personLabel")]
)
data class FaceClusterEntity(
    @PrimaryKey(autoGenerate = true)
    val clusterId: Long = 0,
    val personLabel: String,
    val coverEmbeddingId: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
)