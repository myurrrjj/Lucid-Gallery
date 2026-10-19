package com.example.jetpackcomposegalleryapp.core.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable
import coil.size.pxOrElse

class NativeThumbnailFetcher(
    private val uri: Uri,
    private val context: Context,
    private val isVideo: Boolean,
    private val target: Size
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        target,
                        null
                    )
                } else {
                    val id = uri.lastPathSegment?.toLongOrNull() ?: throw IllegalArgumentException()
                    if (isVideo) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null
                        )
                    } else {
                        MediaStore.Images.Thumbnails.getThumbnail(
                            context.contentResolver,
                            id,
                            MediaStore.Images.Thumbnails.MINI_KIND,
                            null
                        )
                    }
                }

                bitmap?.let {
                    DrawableResult(
                        drawable = bitmap.toDrawable(context.resources),
                        isSampled = true,
                        dataSource = DataSource.DISK
                    )
                }

            }.getOrNull()
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isThumbnail = options.parameters.value("is_thumbnail") as? Boolean ?: false

            if (!isThumbnail || data.scheme != "content" && data.authority != MediaStore.AUTHORITY) {
                return null
            }
            val w = options.size.width.pxOrElse { MAX_EDGE }.coerceIn(1, MAX_EDGE)
            val h = options.size.height.pxOrElse { MAX_EDGE }.coerceIn(1,MAX_EDGE)
            return NativeThumbnailFetcher(data, context, data.isVideoUri(), Size(w, h))

        }


    }
companion object{private const val MAX_EDGE = 512}
}
private fun Uri.isVideoUri() = pathSegments.contains("video")
