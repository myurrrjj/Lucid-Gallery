package com.myurrrjj.chronicle.presentation.gallery.components

import android.net.Uri
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import androidx.core.net.toUri

@Composable
fun ZoomableImage(
    uriString: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    // FIX: now carries the tap offset so the caller can tell which screen half was tapped
    onTap: ((Offset) -> Unit)? = null
) {
    val context = LocalContext.current
    val uri = remember(uriString) { uriString.toUri() }
    val currentOnTap by rememberUpdatedState(onTap)

    val viewState = remember { mutableStateOf<Uri?>(null) }

    AndroidView(
        factory = { ctx ->
            SubsamplingScaleImageView(ctx).apply {
                setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                setMaxScale(5.0f)
                setDoubleTapZoomScale(1.50f)
                orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                isQuickScaleEnabled = true

                // FIX: setOnClickListener alone gives no coordinates, so this listener just
                // FIX: remembers where the finger went down, returning false so pan/pinch/double-tap
                // FIX: zoom inside SubsamplingScaleImageView still work exactly as before
                var lastTapOffset = Offset.Zero
                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        lastTapOffset = Offset(event.x, event.y)
                    }
                    false
                }
                setOnClickListener {
                    currentOnTap?.invoke(lastTapOffset)
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            if (viewState.value != uri) {
                viewState.value = uri
                view.setImage(ImageSource.uri(uri).tilingEnabled())
            }
        }
    )
}