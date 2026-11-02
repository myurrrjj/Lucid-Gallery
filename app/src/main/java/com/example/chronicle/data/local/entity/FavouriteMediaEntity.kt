// app/src/main/java/com/example/jetpackcomposegalleryapp/data/local/entity/FavoriteMediaEntity.kt
package com.example.chronicle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.chronicle.domain.model.MediaAsset

@Entity(tableName = "favourite_media")
data class FavouriteMediaEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val size: Long,
    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val folderName: String?
) {
    fun toDomainModel(): MediaAsset {
        return MediaAsset(
            id = id, uriString = uriString, name = name, dateAdded = dateAdded,
            mimeType = mimeType, size = size, width = width, height = height,
            duration = duration, folderName = folderName
        )
    }
}

fun MediaAsset.toFavoriteEntity(): FavouriteMediaEntity {
    return FavouriteMediaEntity(
        id = id, uriString = uriString, name = name, dateAdded = dateAdded,
        mimeType = mimeType, size = size, width = width, height = height,
        duration = duration, folderName = folderName
    )
}