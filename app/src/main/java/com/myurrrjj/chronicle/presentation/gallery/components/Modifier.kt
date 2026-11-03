package com.myurrrjj.chronicle.presentation.gallery.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.myurrrjj.chronicle.domain.model.GalleryViewMode

@Suppress("AvoidComposed")
fun Modifier.galleryZoomGesture(
    currentMode: GalleryViewMode,
    onViewModeChanged: (GalleryViewMode) -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current

    this.then(
        Modifier.pointerInput(currentMode) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)

                var zoomMultiplier = 1f

                do {
                    val event = awaitPointerEvent()
                    val zoomChange = event.calculateZoom()
                    zoomMultiplier *= zoomChange

                    if (zoomMultiplier > 1.3f) {
                        val newMode = when (currentMode) {
                            GalleryViewMode.YEAR -> GalleryViewMode.MONTH
                            GalleryViewMode.MONTH -> GalleryViewMode.DAY
                            GalleryViewMode.DAY -> GalleryViewMode.DAY
                        }
                        if (newMode != currentMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onViewModeChanged(newMode)

                            event.changes.forEach { it.consume() }
                            break
                        }
                    }
                    else if (zoomMultiplier < 0.7f) {
                        val newMode = when (currentMode) {
                            GalleryViewMode.DAY -> GalleryViewMode.MONTH
                            GalleryViewMode.MONTH -> GalleryViewMode.YEAR
                            GalleryViewMode.YEAR -> GalleryViewMode.YEAR
                        }
                        if (newMode != currentMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onViewModeChanged(newMode)

                            event.changes.forEach { it.consume() }
                            break
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    )
}