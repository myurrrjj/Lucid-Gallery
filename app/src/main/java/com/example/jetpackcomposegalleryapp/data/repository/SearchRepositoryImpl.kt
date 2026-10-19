package com.example.jetpackcomposegalleryapp.data.repository

import com.example.jetpackcomposegalleryapp.data.local.dao.SearchDao
import com.example.jetpackcomposegalleryapp.data.local.entity.SearchIndexEntity
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val searchDao: SearchDao
) : SearchRepository {
    @Volatile private var lastSynced:Int?=null

    override suspend fun syncSearchIndex(
        mediaList: List<MediaAsset>,
        favouriteIds: Set<Long>
    ): Flow<Float> = flow {
        val sync = 31 * mediaList.hashCode() + favouriteIds.hashCode()
        if (sync == lastSynced) {
            emit(1f)
            return@flow
        }
        val total = mediaList.size
        if (total == 0) {
            searchDao.clearIndex()
            lastSynced = sync
            emit(1f)
            return@flow
        }

        val dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy d")
        val zoneId = ZoneId.systemDefault()

        searchDao.clearIndex()

        val chunkSize = 500
        val chunks = mediaList.chunked(chunkSize)
        var processed = 0

        for (batch in chunks) {
            yield()

            val indices = batch.map { media ->
                val searchableTerms = buildString {
                    append(media.name).append(" ")
                    append(media.folderName ?: "Unknown").append(" ")
                    if (media.isVideo) append("video mp4 ") else append("photo image ")
                    if (favouriteIds.contains(media.id)) append("favorite favourites ")

                    val dateStr = Instant.ofEpochSecond(media.dateAdded)
                        .atZone(zoneId)
                        .format(dateFormatter)
                    append(dateStr)
                }

                SearchIndexEntity(
                    mediaId = media.id,
                    terms = searchableTerms.lowercase()
                )
            }

            searchDao.insertAll(indices)
            processed += batch.size

            emit(processed.toFloat() / total.toFloat())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchMediaIds(query: String): List<Long> = withContext(Dispatchers.IO) {
        val matchQuery = buildPrefixMatchQuery(query)
        if (matchQuery.isBlank()) emptyList() else searchDao.searchMediaIds(matchQuery)
    }

    private fun buildPrefixMatchQuery(raw: String): String {
        return raw.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "${it}*" }
    }

}