package com.example.jetpackcomposegalleryapp.core.ml

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
    val face: Face, val accepted: Boolean, val rejectionReasons: List<String>
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
            )/*
                 * We are processing independent still images,
                 * not a video stream.
                 *
                 * Tracking therefore isn't useful here.
                 */.setMinFaceSize(0.03f).build()

        FaceDetection.getClient(options)
    }

    /**
     * Production API.
     *
     * Returns only faces that pass our validation.
     */
    suspend fun detectFaces(
        bitmap: Bitmap
    ): List<Face> {

        return detectFacesWithDiagnostics(bitmap).filter { it.accepted }.map { it.face }
    }

    /**
     * Diagnostic API used by DirtyTest.
     *
     * Returns every face detected by ML Kit,
     * including rejected faces.
     */
    suspend fun detectFacesWithDiagnostics(
        bitmap: Bitmap
    ): List<FaceDetectionDiagnostic> {

        return try {

            val image = InputImage.fromBitmap(
                bitmap, 0
            )

            val rawFaces = faceDetector.process(image).await()

            rawFaces.map { face ->

                val reasons = validationReasons(face)

                FaceDetectionDiagnostic(
                    face = face, accepted = reasons.isEmpty(), rejectionReasons = reasons
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

    /**
     * Recognition-oriented validation.
     *
     * This is intentionally permissive.
     *
     * We do NOT require:
     * - eye landmarks
     * - mouth landmarks
     * - eye-open probability
     * - smiling probability
     *
     * Those are not necessary just to establish
     * that ML Kit detected a face.
     */
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

        /*
         * Very small detections are still rejected,
         * because they are generally not useful for
         * face recognition.
         */
        if (width < 40 || height < 40) {

            reasons += "face too small: ${width}x$height"
        }

        val aspectRatio = height.toFloat() / width.toFloat()

        /*
         * Extremely broad tolerance.
         */
        if (aspectRatio < 0.60f || aspectRatio > 2.50f) {

            reasons += "unusual face-box aspect ratio: %.2f".format(aspectRatio)
        }

        /*
         * Very permissive head-pose limits.
         *
         * Alignment can handle moderate pose differences,
         * so we should not throw away potentially useful faces.
         */
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

        /*
         * If both eyes exist, keep the geometry check
         * only as a very broad sanity check.
         *
         * Missing eyes are completely acceptable here.
         */
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

            if (eyeDistanceRatio < 0.10f || eyeDistanceRatio > 0.90f) {

                reasons += "unusual eye-distance ratio: %.2f".format(
                        eyeDistanceRatio
                    )
            }
        }

        return reasons
    }
}