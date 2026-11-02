package com.example.chronicle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_processing_status")
data class MediaProcessingStatusEntity (
    @PrimaryKey
    val mediaId: Long,
    val processedAt:Long = System.currentTimeMillis(),
    val facesFound :  Int = 0
)