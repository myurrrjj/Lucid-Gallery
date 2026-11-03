package com.myurrrjj.chronicle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myurrrjj.chronicle.data.local.entity.FavouriteMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourite_media ORDER BY dateAdded DESC")
    fun getFavourites(): Flow<List<FavouriteMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(media: FavouriteMediaEntity)

    @Query("DELETE FROM favourite_media WHERE id = :mediaId")
    suspend fun removeFavourite(mediaId: Long)

    @Query("SELECT id FROM favourite_media")
    fun getFavouriteIds(): Flow<List<Long>>
}