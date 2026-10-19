package com.example.jetpackcomposegalleryapp.presentation.gallery.screens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryViewModel
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ControlPosition
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.CustomVideoPlayer
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaControlAlignment
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaInfoSheet
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PlayerControlSurface
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.VideoPlayerState
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.ZoomableImage
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.disableTrackType
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.findActivity
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.formatVideoTime
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.rememberVideoPlayerState
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.selectTrack
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(
    initialMediaId: Long,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentState = state.contentState
    val configState = state.configState
    val infoSheetState = state.infoSheetState
    var isUiVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val initialIndex = remember(initialMediaId, contentState.displayedMediaList) {
        contentState.displayedMediaList.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex, pageCount = { contentState.displayedMediaList.size })

    val currentMedia by remember {
        derivedStateOf {
            contentState.displayedMediaList.getOrNull(pagerState.currentPage)
        }
    }

    val isFavorite = remember(currentMedia, contentState.favoriteMediaIds) {
        currentMedia?.id?.let { contentState.favoriteMediaIds.contains(it) } ?: false
    }

    var controlPosition by remember { mutableStateOf(ControlPosition.BOTTOM) }
    val configuration = LocalConfiguration.current
    val videoIsLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(videoIsLandscape) {
        controlPosition = if (!videoIsLandscape) ControlPosition.BOTTOM else ControlPosition.RIGHT
    }

    val horizontalBias by animateFloatAsState(
        targetValue = when (controlPosition) {
            ControlPosition.LEFT -> -1f
            ControlPosition.RIGHT -> 1f
            ControlPosition.BOTTOM -> 0f
        }, animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow
        ), label = "horizontalBias"
    )
    val verticalBias = 1f

    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val cutoutStart = cutoutPadding.calculateStartPadding(layoutDirection)
    val cutoutEnd = cutoutPadding.calculateEndPadding(layoutDirection)

    val isPagerSwiping by remember { derivedStateOf { pagerState.isScrollInProgress } }

    val mediaControlAlignment = MediaControlAlignment(
        controlPosition = controlPosition,
        onControlPositionChanged = { controlPosition = it },
        horizontalBias = horizontalBias,
        verticalBias = verticalBias,
        cutoutStart = cutoutStart,
        cutoutEnd = cutoutEnd,
        videoIsLandscape = videoIsLandscape,
        isPagerSwiping = isPagerSwiping
    )

    var photoBoxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 16.dp,
            key = { page -> contentState.displayedMediaList[page].id }) { page ->

            val media = contentState.displayedMediaList[page]
            val imageRequest = ImageRequest.Builder(context).data(media.uriString)
                .placeholderMemoryCacheKey(media.uriString).build()
            val isCurrentPage = pagerState.currentPage == page
            val isTransitionRunning = animatedVisibilityScope.transition.isRunning
            val transitionKey = if (isCurrentPage) "media_${media.id}" else "{ghost_key}"

            if (media.isVideo) {
                val exoPlayer = remember { ExoPlayer.Builder(context).build() }
                val videoState = rememberVideoPlayerState(player = exoPlayer)
                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {

                        if (!isTransitionRunning) {
                            CustomVideoPlayer(
                                media = media,
                                animatedVisibilityScope = animatedVisibilityScope,
                                isCurrentPage = isCurrentPage,
                                exoPlayer = exoPlayer,
                                isVisible = isUiVisible,
                                videoState = videoState.value,
                                onVisibilityChanged = { isUiVisible = it },
                                autoPlay = configState.autoPlayVideo,
                                sharedTransitionScope = sharedTransitionScope,
                                isFavorite = isFavorite,
                                onActionClick = { action ->
                                    currentMedia?.let { media ->
                                        viewModel.setEvent(
                                            GalleryEvent.PerformMediaAction(
                                                action, listOf(media)
                                            )
                                        )
                                    }
                                },
                                mediaControlAlignment = mediaControlAlignment
                            )
                        }
                        val imageAlpha by animateFloatAsState(
                            targetValue = if (videoState.value.isFirstFrameRendered) 0f else 1f,
                            animationSpec = tween(500),
                            label = "thumbnail_crossfade"
                        )
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = media.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = transitionKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    renderInOverlayDuringTransition = true
                                )
                                .graphicsLayer { alpha = imageAlpha }
                        )
                    }
                }
            } else {
                with(sharedTransitionScope) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { photoBoxSize = it },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = media.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = transitionKey),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    renderInOverlayDuringTransition = true
                                )
                        )
                        if (!isTransitionRunning) {
                            ZoomableImage(
                                uriString = media.uriString,
                                contentDescription = media.name,
                                onTap = { offset ->
                                    val tappedPosition = if (videoIsLandscape) {
                                        if (offset.x < photoBoxSize.width / 2) ControlPosition.LEFT else ControlPosition.RIGHT
                                    } else {
                                        ControlPosition.BOTTOM
                                    }

                                    if (!isUiVisible) {
                                        controlPosition = tappedPosition
                                        isUiVisible = true
                                    } else if (controlPosition != tappedPosition) {
                                        controlPosition = tappedPosition
                                    } else {
                                        isUiVisible = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f)
                            )
                        }
                    }
                }
            }
        }

        with(sharedTransitionScope) {
            AnimatedVisibility(
                visible = isUiVisible && currentMedia?.isVideo != true,
                enter = if (isPagerSwiping) fadeIn(tween(150)) else fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = if (isPagerSwiping) fadeOut(tween(150)) else fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias, verticalBias))
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                        start = 16.dp + if (controlPosition == ControlPosition.LEFT) cutoutStart else 0.dp,
                        end = 16.dp + if (controlPosition == ControlPosition.RIGHT) cutoutEnd else 0.dp
                    )
                    .zIndex(1f)
            ) {
                DetailFloatingBar(
                    isFavorite = isFavorite, onActionClick = { action ->
                        currentMedia?.let { media ->
                            viewModel.setEvent(
                                GalleryEvent.PerformMediaAction(
                                    action, listOf(media)
                                )
                            )
                        }
                    }, modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                            zIndexInOverlay = 1f
                        )
                )
            }
        }

        MediaInfoSheet(
            sheetState = infoSheetState,
            onDismiss = { viewModel.setEvent(GalleryEvent.CloseInfoSheet) })
    }
}


@Composable
fun VideoPlayerOverlay(
    player: ExoPlayer,
    state: VideoPlayerState,
    isVisible: Boolean,
    onVisibilityChanged: (Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isFavorite: Boolean,
    onActionClick: (DetailAction) -> Unit,
    mediaControlAlignment: MediaControlAlignment,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    var showAudioMenu by remember { mutableStateOf(false) }
    var showSubtitleMenu by remember { mutableStateOf(false) }

    val currentProgress = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val sliderValue = if (isDragging) dragProgress else currentProgress
    val subtitlesOn = state.selectedSubtitleTrack >= 0

    val controlPosition = mediaControlAlignment.controlPosition
    val currentControlPosition by rememberUpdatedState(controlPosition)

    val horizontalBias = mediaControlAlignment.horizontalBias
    val verticalBias = mediaControlAlignment.verticalBias
    val cutoutStart = mediaControlAlignment.cutoutStart
    val cutoutEnd = mediaControlAlignment.cutoutEnd
    val videoIsLandscape = mediaControlAlignment.videoIsLandscape
    val isPagerSwiping = mediaControlAlignment.isPagerSwiping

    LaunchedEffect(
        isVisible, state.isPlaying, isDragging, showAudioMenu, showSubtitleMenu, controlPosition
    ) {
        if (isVisible && state.isPlaying && !isDragging && !showAudioMenu && !showSubtitleMenu) {
            delay(3000L)
            onVisibilityChanged(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isVisible, videoIsLandscape) {
                detectTapGestures(onTap = { offset ->
                    val tappedPosition = if (videoIsLandscape) {
                        if (offset.x < size.width / 2) ControlPosition.LEFT else ControlPosition.RIGHT
                    } else {
                        ControlPosition.BOTTOM
                    }

                    if (!isVisible) {
                        mediaControlAlignment.onControlPositionChanged(tappedPosition)
                        onVisibilityChanged(true)
                    } else {
                        if (currentControlPosition != tappedPosition) {
                            mediaControlAlignment.onControlPositionChanged(tappedPosition)
                        } else {
                            onVisibilityChanged(false)
                        }
                    }
                })
            }) {
        AnimatedVisibility(
            visible = isVisible,
            enter = if (isPagerSwiping) fadeIn(tween(150)) else fadeIn(),
            exit = if (isPagerSwiping) fadeOut(tween(150)) else fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            start = if (controlPosition == ControlPosition.LEFT) cutoutStart else 0.dp,
                            end = if (controlPosition == ControlPosition.RIGHT) cutoutEnd else 0.dp
                        )
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .padding(
                            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                            start = 16.dp,
                            end = 16.dp
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .align(BiasAlignment(horizontalBias, verticalBias))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.audioTracks.isNotEmpty()) {
                                Box {
                                    PlayerControlSurface(
                                        onClick = { showAudioMenu = true },
                                        modifier = Modifier.size(48.dp),
                                        contentDescription = "AudioTrack",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Audiotrack,
                                            contentDescription = "Audio track",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showAudioMenu,
                                        onDismissRequest = { showAudioMenu = false }) {
                                        state.audioTracks.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(text = label) },
                                                trailingIcon = {
                                                    if (index == state.selectedAudioTrack) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectTrack(
                                                        player, C.TRACK_TYPE_AUDIO, index
                                                    )
                                                    showAudioMenu = false
                                                })
                                        }
                                    }
                                }
                            }
                            if (state.subtitleTracks.isNotEmpty()) {
                                Box {
                                    PlayerControlSurface(
                                        onClick = { showSubtitleMenu = true },
                                        modifier = Modifier.size(48.dp),
                                        contentDescription = "SubtitleTrack",
                                        containerColor = if (subtitlesOn) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (subtitlesOn) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                                            contentDescription = "Subtitles",
                                            tint = if (subtitlesOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSubtitleMenu,
                                        onDismissRequest = { showSubtitleMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Off") },
                                            trailingIcon = {
                                                if (!subtitlesOn) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                disableTrackType(player, C.TRACK_TYPE_TEXT)
                                                showSubtitleMenu = false
                                            })
                                        state.subtitleTracks.forEachIndexed { index, label ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                trailingIcon = {
                                                    if (index == state.selectedSubtitleTrack) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectTrack(
                                                        player, C.TRACK_TYPE_TEXT, index
                                                    )
                                                    showSubtitleMenu = false
                                                })
                                        }
                                    }
                                }
                            }
                            PlayerControlSurface(
                                onClick = { if (state.isPlaying) player.pause() else player.play() },
                                modifier = Modifier.size(56.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerControlSurface(
                                onClick = {
                                    val newSpeed = when (state.playbackSpeed) {
                                        1f -> 1.5f; 1.5f -> 2f; 2f -> 0.5f; else -> 1f
                                    }
                                    player.playbackParameters = PlaybackParameters(newSpeed)
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentDescription = "Playback speed"
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${state.playbackSpeed}x",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            PlayerControlSurface(
                                onClick = {
                                    activity?.requestedOrientation = if (!videoIsLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentDescription = "Fullscreen"
                            ) {
                                Icon(
                                    imageVector = if (videoIsLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatVideoTime(if (isDragging) (dragProgress * state.duration).toLong() else state.currentPosition),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = sliderValue, onValueChange = { newValue ->
                                isDragging = true
                                dragProgress = newValue
                            }, onValueChangeFinished = {
                                isDragging = false
                                player.seekTo((dragProgress * state.duration).toLong())
                            }, colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.4f
                                )
                            ), modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatVideoTime(state.duration),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    with(sharedTransitionScope) {
                        DetailFloatingBar(
                            isFavorite = isFavorite,
                            onActionClick = onActionClick,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                zIndexInOverlay = 1f
                            )
                        )
                    }
                }
            }
        }
    }
}