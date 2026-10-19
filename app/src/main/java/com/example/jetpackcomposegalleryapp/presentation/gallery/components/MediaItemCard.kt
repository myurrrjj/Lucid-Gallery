package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    size: Int = 300,
    media: MediaAsset,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    albumAnimatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(media.uriString, media.id, size) {
        ImageRequest.Builder(context).data(media.uriString)
//            .size(size)
            .setParameter("is_thumbnail", true)
            .build()
    }

    val isTiny = size <= 50

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .zIndex(if (sharedTransitionScope.isTransitionActive) 1f else 0f)
                .clip(RoundedCornerShape(if (isTiny) 2.dp else 12.dp))
                .combinedClickable(
                    onClick = onClick, onLongClick = onLongClick
                )
        ) {
            var imageModifier: Modifier = Modifier.fillMaxSize()

            if (!isTiny) {
                imageModifier = imageModifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "media_${media.id}"),
                        animatedVisibilityScope = tabAnimatedVisibilityScope,
                        renderInOverlayDuringTransition = true,
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp))
                    )
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "media_local_${media.id}"),
                        animatedVisibilityScope = albumAnimatedVisibilityScope,
                        renderInOverlayDuringTransition = true,
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
                    )
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                filterQuality = if (isTiny) FilterQuality.None else FilterQuality.Low,
                modifier = imageModifier
            )

            if (!isTiny) {
                val overlayBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.0f),
                            Color.Black.copy(alpha = 0.5f)
                        ), startY = 150f
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayBrush)
                )

                if (media.isVideo) {
                    val formattedDuration = remember(media.duration) {
                        media.duration?.let { formatDuration(it) }
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                    if (formattedDuration != null) {
                        Text(
                            text = formattedDuration,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        )
                    }
                }
            } else if (media.isVideo) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomStart)
                        .padding(2.dp)
                )
            }

            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else Color.Black.copy(alpha = 0.1f)
                        )
                )
                if (!isTiny) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected" else "Unselected",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}