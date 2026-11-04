package com.myurrrjj.benchmark

import android.os.Build
import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.myurrrjj.chronicle.testRelease"
private const val TAG = "GalleryBenchmark"

private const val FIND_TIMEOUT_MS = 5_000L
private const val MAX_GESTURE_ATTEMPTS = 3
private const val SWIPE_STEPS = 20


private const val PINCH_PERCENT = 0.45f
private const val PINCH_SPEED_PX_PER_SEC = 60


private const val IDLE_TIMEOUT_MS = 1_000L
private const val IDLE_GLOBAL_TIMEOUT_MS = 10_000L

private fun MacrobenchmarkScope.grantMediaPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        device.executeShellCommand("pm grant $packageName android.permission.READ_MEDIA_IMAGES")
        device.executeShellCommand("pm grant $packageName android.permission.READ_MEDIA_VIDEO")
    } else {
        device.executeShellCommand("pm grant $packageName android.permission.READ_EXTERNAL_STORAGE")
    }
}

private fun MacrobenchmarkScope.waitForUi() {
    device.waitForIdle(IDLE_TIMEOUT_MS)
}


private fun MacrobenchmarkScope.galleryNode(): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.pkg(TARGET_PACKAGE)), FIND_TIMEOUT_MS)) {
        "No accessibility node found for $TARGET_PACKAGE within ${FIND_TIMEOUT_MS}ms"
    }


private fun MacrobenchmarkScope.pinch(open: Boolean) {
    val name = if (open) "pinchOpen" else "pinchClose"
    var lastError: Throwable? = null

    repeat(MAX_GESTURE_ATTEMPTS) { attempt ->
        try {
            galleryNode().apply {
                setGestureMargin(device.displayWidth / 10)
                if (open) {
                    pinchOpen(PINCH_PERCENT, PINCH_SPEED_PX_PER_SEC)
                } else {
                    pinchClose(PINCH_PERCENT, PINCH_SPEED_PX_PER_SEC)
                }
            }
            waitForUi()
            return
        } catch (e: StaleObjectException) {
            lastError = e
            Log.w(
                TAG,
                "$name hit a stale node (attempt ${attempt + 1}/$MAX_GESTURE_ATTEMPTS), re-resolving",
            )
            waitForUi()
        }
    }

    throw AssertionError("$name failed after $MAX_GESTURE_ATTEMPTS attempts: node kept going stale", lastError)
}

@RunWith(AndroidJUnit4::class)
class GalleryCombinedPerformanceBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun scrollAndPinchFullJourney() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            MemoryUsageMetric(mode = MemoryUsageMetric.Mode.Max)
        ),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            grantMediaPermissions()
            pressHome()
            startActivityAndWait()
            waitForUi()
        }
    ) {
        val centerX = device.displayWidth / 2
        val bottomY = (device.displayHeight * 0.78f).toInt()
        val topY = (device.displayHeight * 0.25f).toInt()

        fun scrollContentUp(times: Int) = repeat(times) {
            device.swipe(centerX, bottomY, centerX, topY, SWIPE_STEPS)
            waitForUi()
        }

        fun scrollContentDown(times: Int) = repeat(times) {
            device.swipe(centerX, topY, centerX, bottomY, SWIPE_STEPS)
            waitForUi()
        }

        scrollContentUp(3)
        scrollContentDown(3)

        pinch(open = false)
        pinch(open = false)

        scrollContentUp(2)
        scrollContentDown(2)

        pinch(open = true)
        pinch(open = true)
    }
}