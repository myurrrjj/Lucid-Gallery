package com.example.chronicle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chronicle.data.local.dao.FaceDao
import com.example.chronicle.data.local.dao.FavouriteDao
import com.example.chronicle.data.local.dao.SearchDao
import com.example.chronicle.data.local.entity.FaceClusterEntity
import com.example.chronicle.data.local.entity.FaceEmbeddingEntity
import com.example.chronicle.data.local.entity.FavouriteMediaEntity
import com.example.chronicle.data.local.entity.MediaPersonEntity
import com.example.chronicle.data.local.entity.MediaProcessingStatusEntity
import com.example.chronicle.data.local.entity.SearchIndexEntity

@Database(
    entities = [
        FavouriteMediaEntity::class,
        FaceEmbeddingEntity::class,
        FaceClusterEntity::class,
        MediaPersonEntity::class,
        MediaProcessingStatusEntity::class,
        SearchIndexEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract val favouriteDao: FavouriteDao
    abstract val faceDao : FaceDao
    abstract val searchDao: SearchDao


    companion object {
        @Volatile
        private var INSTANCE: GalleryDatabase? = null

        fun getInstance(context: Context): GalleryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, GalleryDatabase::class.java, "gallery_db"
                )
                    .fallbackToDestructiveMigration()
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}