package com.example.jetpackcomposegalleryapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_person",
    indices = [
        Index("embeddingId"),
        Index("clusterId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = FaceEmbeddingEntity::class,
            parentColumns = ["embeddingId"],
            childColumns = ["embeddingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FaceClusterEntity::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaPersonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val embeddingId: Long,
    val clusterId: Long
)