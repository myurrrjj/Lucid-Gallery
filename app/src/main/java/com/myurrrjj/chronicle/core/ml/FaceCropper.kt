package com.myurrrjj.chronicle.core.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import androidx.core.graphics.scale

object FaceCropper {


    private val ARC_FACE_TEMPLATE = arrayOf(
        floatArrayOf(38.2946f, 51.6963f),
        floatArrayOf(73.5318f, 51.5014f),
        floatArrayOf(56.0252f, 71.7366f),
        floatArrayOf(41.5493f, 92.3655f),
        floatArrayOf(70.7299f, 92.2041f)
    )

    fun cropFace(
        originalBitmap: Bitmap,
        face: Face,
        margin: Float = 0.2f
    ): Bitmap? {


        val aligned = alignFace(originalBitmap, face)

        if (aligned != null) {
            return aligned
        }


        return cropFaceByBoundingBox(
            originalBitmap = originalBitmap,
            face = face,
            margin = margin
        )
    }

    private fun alignFace(
        originalBitmap: Bitmap,
        face: Face
    ): Bitmap? {

        val leftEye =
            face.getLandmark(FaceLandmark.LEFT_EYE)?.position

        val rightEye =
            face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        val nose =
            face.getLandmark(FaceLandmark.NOSE_BASE)?.position

        val leftMouth =
            face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position

        val rightMouth =
            face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position


        if (
            leftEye == null ||
            rightEye == null ||
            nose == null ||
            leftMouth == null ||
            rightMouth == null
        ) {
            return null
        }

        val sourcePoints = arrayOf(
            floatArrayOf(leftEye.x, leftEye.y),
            floatArrayOf(rightEye.x, rightEye.y),
            floatArrayOf(nose.x, nose.y),
            floatArrayOf(leftMouth.x, leftMouth.y),
            floatArrayOf(rightMouth.x, rightMouth.y)
        )

        val transform = calculateSimilarityTransform(
            sourcePoints = sourcePoints,
            targetPoints = ARC_FACE_TEMPLATE
        ) ?: return null

        return try {
            val output = Bitmap.createBitmap(
                112,
                112,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(output)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }

            canvas.drawBitmap(
                originalBitmap,
                transform,
                paint
            )

            output
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun calculateSimilarityTransform(
        sourcePoints: Array<FloatArray>,
        targetPoints: Array<FloatArray>
    ): Matrix? {

        if (sourcePoints.size != targetPoints.size || sourcePoints.size < 2) {
            return null
        }

        var sourceCenterX = 0.0
        var sourceCenterY = 0.0
        var targetCenterX = 0.0
        var targetCenterY = 0.0

        for (i in sourcePoints.indices) {
            sourceCenterX += sourcePoints[i][0]
            sourceCenterY += sourcePoints[i][1]

            targetCenterX += targetPoints[i][0]
            targetCenterY += targetPoints[i][1]
        }

        val count = sourcePoints.size.toDouble()

        sourceCenterX /= count
        sourceCenterY /= count

        targetCenterX /= count
        targetCenterY /= count

        var numeratorA = 0.0
        var numeratorB = 0.0
        var denominator = 0.0

        for (i in sourcePoints.indices) {

            val sx = sourcePoints[i][0] - sourceCenterX
            val sy = sourcePoints[i][1] - sourceCenterY

            val tx = targetPoints[i][0] - targetCenterX
            val ty = targetPoints[i][1] - targetCenterY

            numeratorA += sx * tx + sy * ty
            numeratorB += sx * ty - sy * tx

            denominator += sx * sx + sy * sy
        }

        if (denominator <= 1e-8) {
            return null
        }

        val a = numeratorA / denominator
        val b = numeratorB / denominator

        val translationX =
            targetCenterX - (a * sourceCenterX - b * sourceCenterY)

        val translationY =
            targetCenterY - (b * sourceCenterX + a * sourceCenterY)


        return Matrix().apply {
            setValues(
                floatArrayOf(
                    a.toFloat(),
                    (-b).toFloat(),
                    translationX.toFloat(),

                    b.toFloat(),
                    a.toFloat(),
                    translationY.toFloat(),

                    0f,
                    0f,
                    1f
                )
            )
        }
    }

    private fun cropFaceByBoundingBox(
        originalBitmap: Bitmap,
        face: Face,
        margin: Float
    ): Bitmap? {

        val boundingBox = face.boundingBox

        val width = boundingBox.width()
        val height = boundingBox.height()

        if (width <= 0 || height <= 0) {
            return null
        }

        val paddingX = (width * margin).toInt()
        val paddingY = (height * margin).toInt()

        val left = (boundingBox.left - paddingX)
            .coerceIn(0, originalBitmap.width - 1)

        val top = (boundingBox.top - paddingY)
            .coerceIn(0, originalBitmap.height - 1)

        val right = (boundingBox.right + paddingX)
            .coerceIn(left + 1, originalBitmap.width)

        val bottom = (boundingBox.bottom + paddingY)
            .coerceIn(top + 1, originalBitmap.height)

        return try {
            Bitmap.createBitmap(
                originalBitmap,
                left,
                top,
                right - left,
                bottom - top
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun preprocessForEmbedding(faceBitmap: Bitmap): Bitmap {
        return faceBitmap.scale(112, 112)
    }
}