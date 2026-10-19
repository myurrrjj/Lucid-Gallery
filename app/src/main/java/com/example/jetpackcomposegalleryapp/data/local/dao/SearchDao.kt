package com.example.jetpackcomposegalleryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jetpackcomposegalleryapp.data.local.entity.SearchIndexEntity

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(indices : List<SearchIndexEntity>)

    @Query("SELECT mediaId FROM search_index WHERE search_index MATCH :matchQuery")
    suspend fun searchMediaIds(matchQuery : String) : List<Long>

    @Query("DELETE FROM search_index")
    suspend fun clearIndex()
}