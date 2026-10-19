package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.presentation.gallery.screens.VideoPlayerOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class ControlPosition {
    BOTTOM, LEFT, RIGHT
}

data class MediaControlAlignment(
    val controlPosition: ControlPosition,
    val onControlPositionChanged: (ControlPosition) -> Unit,
    val horizontalBias: Float,
    val verticalBias: Float,
    val cutoutStart: Dp,
    val cutoutEnd: Dp,
    val videoIsLandscape: Boolean,
    val isPagerSwiping: Boolean
)

data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
    val audioTracks: List<String> = emptyList(),
    val subtitleTracks: List<String> = emptyList(),
    val selectedAudioTrack: Int = -1,
    val selectedSubtitleTrack: Int = -1,
    val isFirstFrameRendered: Boolean = false
)

@Suppress("MultipleContentEmitters")
@Composable
fun rememberVideoPlayerState(player: ExoPlayer): State<VideoPlayerState> {
    val state = remember { mutableStateOf(VideoPlayerState()) }

    DisposableEffect(player) {
        @Suppress("DerivedStateOfCandidate") val listener = object : Player.Listener {
            @OptIn(UnstableApi::class)
            override fun onRenderedFirstFrame() {
                state.value = state.value.copy(isFirstFrameRendered = true)
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                val subtitleGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }

                state.value = state.value.copy(
                    audioTracks = audioGroups.mapIndexed { index, group ->
                        trackLabel(
                            group,
                            index
                        )
                    },
                    subtitleTracks = subtitleGroups.mapIndexed { index, group ->
                        trackLabel(
                            group,
                            index
                        )
                    },
                    selectedAudioTrack = audioGroups.indexOfFirst { it.isSelected },
                    selectedSubtitleTrack = subtitleGroups.indexOfFirst { it.isSelected }
                )
            }

            override fun onEvents(player: Player, events: Player.Events) {
                state.value = state.value.copy(
                    isPlaying = player.isPlaying,
                    playbackState = player.playbackState,
                    currentPosition = player.currentPosition.coerceAtLeast(0L),
                    duration = player.duration.coerceAtLeast(0L),
                    playbackSpeed = player.playbackParameters.speed
                )
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(state.value.isPlaying) {
        while (isActive && state.value.isPlaying) {
            state.value =
                state.value.copy(currentPosition = player.currentPosition.coerceAtLeast(0L))
            delay(200L)
        }
    }
    return state
}

@OptIn(UnstableApi::class)
private fun trackLabel(group: Tracks.Group, fallbackIndex: Int): String {
    if (group.mediaTrackGroup.length == 0) return "Track ${fallbackIndex + 1}"
    val format = group.mediaTrackGroup.getFormat(0)
    return format.label ?: format.language?.uppercase() ?: "Track ${fallbackIndex + 1}"
}

@Composable
fun PlayerControlSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = CircleShape,
    containerColor: Color,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .bouncyClick(onClick = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

fun formatVideoTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun selectTrack(player: ExoPlayer, trackType: Int, index: Int) {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType }
    if (index in trackGroups.indices) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(TrackSelectionOverride(trackGroups[index].mediaTrackGroup, 0))
            .build()
    }
}

fun disableTrackType(player: ExoPlayer, trackType: Int) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(trackType, true)
        .clearOverridesOfType(trackType)
        .build()
}

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    media: MediaAsset,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isCurrentPage: Boolean,
    exoPlayer: ExoPlayer,
    isVisible: Boolean,
    videoState: VideoPlayerState,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    sharedTransitionScope: SharedTransitionScope,
    isFavorite: Boolean,
    onActionClick: (DetailAction) -> Unit,
    // FIX: was 7 separate params (controlPosition, onControlPositionChanged, horizontalBias,
    // FIX: verticalBias, cutoutStart, cutoutEnd, videoIsLandscape, isPagerSwiping), now 1
    mediaControlAlignment: MediaControlAlignment
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isCurrentPage, media, autoPlay) {
        if (isCurrentPage) {
            val mediaItem = MediaItem.fromUri(media.uriString)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            if (autoPlay) {
                exoPlayer.play()
            }
        } else {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isCurrentPage) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val themeBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    useArtwork = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(themeBackgroundColor)
                }
            },
            update = { playerView ->
                if (isCurrentPage) {
                    playerView.player = exoPlayer
                    playerView.setBackgroundColor(themeBackgroundColor)
                } else {
                    playerView.player = null
                }
            }
        )

        if (isCurrentPage) {
            VideoPlayerOverlay(
                player = exoPlayer,
                state = videoState,
                isVisible = isVisible,
                onVisibilityChanged = onVisibilityChanged,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                isFavorite = isFavorite,
                onActionClick = onActionClick,
                mediaControlAlignment = mediaControlAlignment
            )
        }
    }
}