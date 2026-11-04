package com.myurrrjj.chronicle.core.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class FaceDetectionDiagnostic(
    val face: Face, val accepted: Boolean, val rejectionReasons: List<String>,val qualityMetrics: FaceQualityMetrics?=null
)

@Singleton
class MlKitFaceDetector @Inject constructor() {

    private val faceDetector by lazy {

        val options = FaceDetectorOptions.Builder().setPerformanceMode(
            FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
        ).setLandmarkMode(
            FaceDetectorOptions.LANDMARK_MODE_ALL
        ).setClassificationMode(
            FaceDetectorOptions.CLASSIFICATION_MODE_ALL
        ).setMinFaceSize(0.03f).build()

        FaceDetection.getClient(options)
    }


    suspend fun detectFaces(
        bitmap: Bitmap
    ): List<Face> {

        return detectFacesWithDiagnostics(bitmap).filter { it.accepted }.map { it.face }
    }


    suspend fun detectFacesWithDiagnostics(
        bitmap: Bitmap,
        includeQualityMetrics: Boolean = false
    ): List<FaceDetectionDiagnostic> {

        return try {

            val image = InputImage.fromBitmap(
                bitmap, 0
            )

            val rawFaces = faceDetector.process(image).await()

            rawFaces.map { face ->

                val reasons = validationReasons(face)
                val qualityMetrics = if (includeQualityMetrics){
                    val landmarkCount = listOf(
                        FaceLandmark.LEFT_EYE,
                        FaceLandmark.RIGHT_EYE,
                        FaceLandmark.NOSE_BASE,
                        FaceLandmark.MOUTH_LEFT,
                        FaceLandmark.MOUTH_RIGHT
                    ).count {
                        face.getLandmark(it)!=null
                    }
                    FaceQualityAssessor.assess(
                        bitmap = bitmap,
                        faceBounds = face.boundingBox,
                        alignmentLandmarkCount = landmarkCount
                    )
                }
                else{
                    null
                }

                FaceDetectionDiagnostic(
                    face = face, accepted = reasons.isEmpty(), rejectionReasons = reasons,qualityMetrics = qualityMetrics
                )
            }

        } catch (e: Exception) {

            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }

            e.printStackTrace()

            emptyList()
        }
    }

    fun close() {
        faceDetector.close()
    }


    private fun validationReasons(
        face: Face
    ): List<String> {

        val reasons = mutableListOf<String>()

        val bounds = face.boundingBox

        val width = bounds.width()

        val height = bounds.height()

        if (width <= 0 || height <= 0) {

            reasons += "invalid bounding box"

            return reasons
        }


        if (width < 40 || height < 40) {

            reasons += "face too small: ${width}x$height"
        }

        val aspectRatio = height.toFloat() / width.toFloat()


        if (aspectRatio !in 0.60f..2.50f) {

            reasons += "unusual face-box aspect ratio: %.2f".format(aspectRatio)
        }


        if (abs(face.headEulerAngleX) > 60f) {

            reasons += "head X angle too large: %.1f°".format(
                face.headEulerAngleX
            )
        }

        if (abs(face.headEulerAngleY) > 60f) {

            reasons += "head Y angle too large: %.1f°".format(
                face.headEulerAngleY
            )
        }

        if (abs(face.headEulerAngleZ) > 60f) {

            reasons += "head Z angle too large: %.1f°".format(
                face.headEulerAngleZ
            )
        }


        val leftEye = face.getLandmark(
            FaceLandmark.LEFT_EYE
        )?.position

        val rightEye = face.getLandmark(
            FaceLandmark.RIGHT_EYE
        )?.position

        if (leftEye != null && rightEye != null) {

            val eyeDistance = abs(
                leftEye.x - rightEye.x
            )

            val eyeDistanceRatio = eyeDistance / width.toFloat()

            if (eyeDistanceRatio !in 0.10f..0.90f) {

                reasons += "unusual eye-distance ratio: %.2f".format(
                    eyeDistanceRatio
                )
            }
        }

        return reasons
    }
}