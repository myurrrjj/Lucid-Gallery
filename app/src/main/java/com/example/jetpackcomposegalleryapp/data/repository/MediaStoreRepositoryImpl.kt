package com.example.jetpackcomposegalleryapp.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.example.jetpackcomposegalleryapp.data.local.dao.FavouriteDao
import com.example.jetpackcomposegalleryapp.data.local.entity.toFavoriteEntity
import com.example.jetpackcomposegalleryapp.domain.model.DetailedMediaInfo
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaStoreRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver, private val favouriteDao: FavouriteDao
) : MediaRepository {
    override fun getAllMedia(): Flow<List<MediaAsset>> = flow {
        val mediaList = mutableListOf<MediaAsset>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mimeTypeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val widthColumn = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val bucketColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)

                    val width = if (widthColumn != -1 && !cursor.isNull(widthColumn)) cursor.getInt(
                        widthColumn
                    ) else null
                    val height =
                        if (heightColumn != -1 && !cursor.isNull(heightColumn)) cursor.getInt(
                            heightColumn
                        ) else null
                    val folderName = cursor.getString(bucketColumn) ?: "Unknown"
                    val duration =
                        if (durationColumn != -1 && !cursor.isNull(durationColumn)) cursor.getLong(
                            durationColumn
                        ) else null

                    val contentUri = ContentUris.withAppendedId(
                        if (mimeType.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id

                    )
                    mediaList.add(
                        MediaAsset(
                            id = id,
                            uriString = contentUri.toString(),
                            name = name,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            size = size,
                            width = width,
                            height = height,
                            duration = duration,
                            folderName = folderName

                        )
                    )
                }

            }

          val masterIds = mediaList.map { it.id }.toSet()
        val currentFavs = favouriteDao.getFavourites().first()
        for(fav in currentFavs){
            if(fav.id !in masterIds){
                favouriteDao.removeFavourite(fav.id)
            }
        }
        emit(mediaList)
    }.flowOn(Dispatchers.IO)

    override fun getFavorites(): Flow<List<MediaAsset>> {
        return favouriteDao.getFavourites().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun toggleFavourite(
        media: MediaAsset, isFavourite: Boolean
    ) {
        if (isFavourite) {
            favouriteDao.insertFavourite(media.toFavoriteEntity())
        } else {
            favouriteDao.removeFavourite(media.id)
        }
    }

    override suspend fun getMediaDetails(
        uriString: String, isVideo: Boolean
    ): DetailedMediaInfo = withContext(Dispatchers.IO) {
        val uri = uriString.toUri()
        var info = DetailedMediaInfo()
        try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val width =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toFloatOrNull()
                    val height =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toFloatOrNull()
                    val frameRate =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                            ?.toFloatOrNull()

                    val bitrate =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.toFloatOrNull()
                    val durationMs =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()

//                    val format = retriever.extractMetadata(MediaMetadataRetriever.)
                    val durationFormatted = durationMs?.let {
                        val seconds = (it / 1000) % 60
                        val minutes = (it / (1000 * 60)) % 60
                        String.format("%02d:%02d", minutes, seconds)

                    }
                    info = info.copy(
                        resolution = if (width != null && height != null) "${width}x${height}" else null,
                        frameRate = frameRate,
                        bitrate = bitrate?.let { "${it / 1000} kbps" },
                        durationFormatted = durationFormatted
                    )

                }
                retriever.release()

            } else {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)

                    val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    val exposureNum = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                    info = info.copy(
                        resolution = if (width > 0 && height > 0) "${width}x${height}" else null,
                        megapixelCount = if (width > 0 && height > 0) (width * height) / 1000000f else null,
                        cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                        cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                        aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                        exposureTime = if (exposureNum > 0) "1/${(1 / exposureNum).toInt()}s" else null,
                        iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                            ?.let { "ISO $it" },
                        focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                            .let {
                                if (it > 0) "${it}mm" else null
                            })
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }


        return@withContext info
    }

}

