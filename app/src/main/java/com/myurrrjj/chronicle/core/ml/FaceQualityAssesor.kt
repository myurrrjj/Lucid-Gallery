
package com.myurrrjj.chronicle.core.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.sqrt

data class FaceQualityMetrics(
    val faceWidthPx: Int,
    val faceHeightPx: Int,
    val wasClippedToImage: Boolean,
    val laplacianVariance: Double,
    val meanLuminance: Double,
    val luminanceStdDev: Double,
    val alignmentLandmarkCount: Int
)

object FaceQualityAssessor {

    private const val SAMPLE_EDGE = 64


    fun assess(
        bitmap: Bitmap,
        faceBounds: Rect,
        alignmentLandmarkCount: Int
    ): FaceQualityMetrics? {

        if (bitmap.isRecycled) return null

        if (bitmap.config == Bitmap.Config.HARDWARE) return null

        val originalWidth = faceBounds.width()
        val originalHeight = faceBounds.height()

        if (originalWidth < 3 || originalHeight < 3) {
            return null
        }

        val clippedBounds = Rect(faceBounds)

        val intersects = clippedBounds.intersect(
            0,
            0,
            bitmap.width,
            bitmap.height
        )

        if (!intersects) return null

        if (clippedBounds.width() < 3 || clippedBounds.height() < 3) {
            return null
        }

        val wasClipped =
            clippedBounds.left != faceBounds.left ||
                    clippedBounds.top != faceBounds.top ||
                    clippedBounds.right != faceBounds.right ||
                    clippedBounds.bottom != faceBounds.bottom

        val sampleBitmap = try {
            Bitmap.createBitmap(
                SAMPLE_EDGE,
                SAMPLE_EDGE,
                Bitmap.Config.ARGB_8888
            )
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: IllegalStateException) {
            return null
        }

        return try {

            val canvas = Canvas(sampleBitmap)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                isFilterBitmap = true
            }


            canvas.drawBitmap(
                bitmap,
                clippedBounds,
                Rect(0, 0, SAMPLE_EDGE, SAMPLE_EDGE),
                paint
            )

            val pixels = IntArray(SAMPLE_EDGE * SAMPLE_EDGE)

            sampleBitmap.getPixels(
                pixels,
                0,
                SAMPLE_EDGE,
                0,
                0,
                SAMPLE_EDGE,
                SAMPLE_EDGE
            )

            val grayscale = FloatArray(pixels.size)

            var luminanceSum = 0.0
            var luminanceSquaredSum = 0.0

            for (index in pixels.indices) {

                val pixel = pixels[index]

                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                val luminance =
                    0.299 * red +
                            0.587 * green +
                            0.114 * blue

                grayscale[index] = luminance.toFloat()

                luminanceSum += luminance
                luminanceSquaredSum += luminance * luminance
            }

            val pixelCount = pixels.size.toDouble()

            val meanLuminance =
                luminanceSum / pixelCount

            val luminanceVariance = (
                    luminanceSquaredSum / pixelCount -
                            meanLuminance * meanLuminance
                    ).coerceAtLeast(0.0)

            val luminanceStdDev =
                sqrt(luminanceVariance)

            var laplacianSum = 0.0
            var laplacianSquaredSum = 0.0
            var laplacianCount = 0

            for (y in 1 until SAMPLE_EDGE - 1) {

                for (x in 1 until SAMPLE_EDGE - 1) {

                    val index = y * SAMPLE_EDGE + x

                    val center = grayscale[index].toDouble()

                    val laplacian =
                        grayscale[index - 1] +
                                grayscale[index + 1] +
                                grayscale[index - SAMPLE_EDGE] +
                                grayscale[index + SAMPLE_EDGE] -
                                4.0 * center

                    laplacianSum += laplacian
                    laplacianSquaredSum +=
                        laplacian * laplacian

                    laplacianCount++
                }
            }

            val laplacianMean =
                laplacianSum / laplacianCount

            val laplacianVariance = (
                    laplacianSquaredSum / laplacianCount -
                            laplacianMean * laplacianMean
                    ).coerceAtLeast(0.0)

            FaceQualityMetrics(
                faceWidthPx = originalWidth,
                faceHeightPx = originalHeight,
                wasClippedToImage = wasClipped,
                laplacianVariance = laplacianVariance,
                meanLuminance = meanLuminance,
                luminanceStdDev = luminanceStdDev,
                alignmentLandmarkCount =
                    alignmentLandmarkCount.coerceIn(0, 5)
            )

        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        } finally {
            sampleBitmap.recycle()
        }
    }
}
