
package com.myurrrjj.chronicle.core.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaceQualityAssessorTest {

    @Test
    fun flatImageHasZeroSharpness() {

        val bitmap = Bitmap.createBitmap(
            64,
            64,
            Bitmap.Config.ARGB_8888
        )

        bitmap.eraseColor(Color.rgb(128, 128, 128))

        try {

            val metrics = FaceQualityAssessor.assess(
                bitmap = bitmap,
                faceBounds = Rect(0, 0, 64, 64),
                alignmentLandmarkCount = 5
            )

            assertNotNull(metrics)

            assertEquals(
                0.0,
                metrics!!.laplacianVariance,
                0.000001
            )

            assertEquals(
                128.0,
                metrics.meanLuminance,
                0.1
            )

        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun sharpCheckerboardHasHigherSharpnessThanFlatImage() {

        val bitmap = Bitmap.createBitmap(
            64,
            64,
            Bitmap.Config.ARGB_8888
        )

        try {

            for (y in 0 until 64) {
                for (x in 0 until 64) {

                    val color =
                        if ((x / 4 + y / 4) % 2 == 0) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }

                    bitmap.setPixel(x, y, color)
                }
            }

            val metrics = FaceQualityAssessor.assess(
                bitmap = bitmap,
                faceBounds = Rect(0, 0, 64, 64),
                alignmentLandmarkCount = 5
            )

            assertNotNull(metrics)

            assertTrue(
                metrics!!.laplacianVariance > 0.0
            )

        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun outsideBoundsAreClippedSafely() {

        val bitmap = Bitmap.createBitmap(
            64,
            64,
            Bitmap.Config.ARGB_8888
        )

        try {

            val metrics = FaceQualityAssessor.assess(
                bitmap = bitmap,
                faceBounds = Rect(-10, -10, 40, 40),
                alignmentLandmarkCount = 3
            )

            assertNotNull(metrics)

            assertTrue(metrics!!.wasClippedToImage)

            assertEquals(
                3,
                metrics.alignmentLandmarkCount
            )

        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun invalidSmallBoundsReturnNull() {
        val bitmap = Bitmap.createBitmap(
            64,
            64,
            Bitmap.Config.ARGB_8888
        )
        try {
            val metrics = FaceQualityAssessor.assess(
                bitmap = bitmap,
                faceBounds = Rect(0, 0, 2, 2),
                alignmentLandmarkCount = 0
            )
            assertNull(metrics)
        } finally {
            bitmap.recycle()
        }
    }
}
