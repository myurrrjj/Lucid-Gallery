package com.example.chronicle.di

import android.content.Context
import com.example.chronicle.data.local.GalleryDatabase
import com.example.chronicle.data.local.dao.FaceDao
import com.example.chronicle.data.local.dao.FavouriteDao
import com.example.chronicle.data.local.dao.SearchDao
//import com.example.jetpackcomposegalleryapp.data.local.dao.MediaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideGalleryDatabase(
        @ApplicationContext context: Context
    ): GalleryDatabase {
        return GalleryDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideMediaDao(database: GalleryDatabase): FavouriteDao {
        return database.favouriteDao
    }

    @Provides
    @Singleton
    fun provideFaceDao(database: GalleryDatabase): FaceDao {
        return database.faceDao
    }

    @Provides
    @Singleton
    fun provideSearchDao(database: GalleryDatabase): SearchDao {
        return database.searchDao
    }
}