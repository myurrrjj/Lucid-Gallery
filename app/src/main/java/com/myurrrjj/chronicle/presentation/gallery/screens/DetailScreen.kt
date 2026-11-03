package com.myurrrjj.chronicle.presentation.gallery.screens

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.SubtitlesOff
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.myurrrjj.chronicle.presentation.gallery.GalleryViewModel
import com.myurrrjj.chronicle.presentation.gallery.components.ControlPosition
import com.myurrrjj.chronicle.presentation.gallery.components.CustomVideoPlayer
import com.myurrrjj.chronicle.presentation.gallery.components.DetailAction
import com.myurrrjj.chronicle.presentation.gallery.components.DetailFloatingBar
import com.myurrrjj.chronicle.presentation.gallery.components.MediaControlAlignment
import com.myurrrjj.chronicle.presentation.gallery.components.MediaInfoSheet
import com.myurrrjj.chronicle.presentation.gallery.components.PlayerControlSurface
import com.myurrrjj.chronicle.presentation.gallery.components.VideoPlayerState
import com.myurrrjj.chronicle.presentation.gallery.components.VideoResizeMode
import com.myurrrjj.chronicle.presentation.gallery.components.ZoomableImage
import com.myurrrjj.chronicle.presentation.gallery.components.disableTrackType
import com.myurrrjj.chronicle.presentation.gallery.components.findActivity
import com.myurrrjj.chronicle.presentation.gallery.components.formatVideoTime
import com.myurrrjj.chronicle.presentation.gallery.components.rememberVideoPlayerState
import com.myurrrjj.chronicle.presentation.gallery.components.selectTrack
import com.myurrrjj.chronicle.presentation.gallery.contract.GalleryEvent
import kotlinx.coroutines.delay


import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat


fun Activity.enforceImmersiveMode(enforce: Boolean) {
    val window = window ?: return
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    if (enforce) {
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

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
    var videoResizeMode by rememberSaveable {
        mutableStateOf(VideoResizeMode.FIT)
    }
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
                                resizeMode = videoResizeMode,

                                onResizeModeChanged = { newMode ->
                                    videoResizeMode = newMode
                                },
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
                                .graphicsLayer { alpha = imageAlpha })
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
                enter = if (isPagerSwiping) fadeIn(tween(150)) else fadeIn() + slideInVertically(
                    initialOffsetY = { it }),
                exit = if (isPagerSwiping) fadeOut(tween(150)) else fadeOut() + slideOutVertically(
                    targetOffsetY = { it }),
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
                    }, modifier = Modifier.sharedBounds(
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

private enum class OverlayPanel {
    NONE, TRACKS, SPEED
}

@Composable
fun VideoPlayerOverlay(
    player: ExoPlayer,
    state: VideoPlayerState,
    isVisible: Boolean,
    resizeMode: VideoResizeMode,
    onResizeModeChanged: (VideoResizeMode) -> Unit,
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
    DisposableEffect(Unit) {
        activity?.enforceImmersiveMode(true)
        onDispose {
            activity?.enforceImmersiveMode(false)
        }
    }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    var activePanel by remember { mutableStateOf(OverlayPanel.NONE) }

    LaunchedEffect(isVisible) { if (!isVisible) activePanel = OverlayPanel.NONE }

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
        isVisible,
        state.isPlaying,
        isDragging,
        activePanel,
        controlPosition
    ) {
        if (isVisible && state.isPlaying && !isDragging && activePanel == OverlayPanel.NONE) {
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
            }

    ) {
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
                                Color.Black.copy(alpha = 0.7f)
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
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .padding(
                            bottom = WindowInsets.systemBars.asPaddingValues()
                                .calculateBottomPadding() + 8.dp,
                            start = 12.dp,
                            end = 12.dp
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .align(BiasAlignment(horizontalBias, verticalBias))
                ) {

                    AnimatedVisibility(
                        visible = activePanel != OverlayPanel.NONE,
                        enter = expandVertically() + fadeIn(tween(150)),
                        exit = shrinkOut() + fadeOut(tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (activePanel) {
                                OverlayPanel.TRACKS -> {
                                    if (state.subtitleTracks.isNotEmpty()) {
                                        Text(
                                            text = "Subtitles",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OptionPill(
                                                label = "Off",
                                                selected = !subtitlesOn,
                                                onClick = {
                                                    disableTrackType(player, C.TRACK_TYPE_TEXT)
                                                }
                                            )
                                            state.subtitleTracks.forEachIndexed { index, label ->
                                                OptionPill(
                                                    label = label,
                                                    selected = index == state.selectedSubtitleTrack,
                                                    onClick = {
                                                        selectTrack(player, C.TRACK_TYPE_TEXT, index)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (state.audioTracks.isNotEmpty()) {
                                        Text(
                                            text = "Audio Track",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            state.audioTracks.forEachIndexed { index, label ->
                                                OptionPill(
                                                    label = label,
                                                    selected = index == state.selectedAudioTrack,
                                                    onClick = {
                                                        selectTrack(player, C.TRACK_TYPE_AUDIO, index)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                OverlayPanel.SPEED -> {
                                    Text(
                                        text = "Playback Speed",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        speeds.forEach { speed ->
                                            OptionPill(
                                                label = "${speed}x",
                                                selected = speed == state.playbackSpeed,
                                                onClick = {
                                                    player.playbackParameters = PlaybackParameters(speed)
                                                    activePanel = OverlayPanel.NONE
                                                }
                                            )
                                        }
                                    }
                                }

                                OverlayPanel.NONE -> Unit
                            }
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerControlSurface(
                                onClick = { if (state.isPlaying) player.pause() else player.play() },
                                modifier = Modifier.size(46.dp),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause Button",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            val hasTracks = state.audioTracks.isNotEmpty() || state.subtitleTracks.isNotEmpty()
                            if (hasTracks) {
                                val isTracksMenuOpened = activePanel == OverlayPanel.TRACKS
                                PlayerControlSurface(
                                    onClick = {
                                        activePanel = if (isTracksMenuOpened) OverlayPanel.NONE else OverlayPanel.TRACKS
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    containerColor = when {
                                        isTracksMenuOpened -> MaterialTheme.colorScheme.primary
                                        subtitlesOn -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentDescription = "Audio & Subtitles"
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val iconTint = when {
                                            isTracksMenuOpened -> MaterialTheme.colorScheme.onPrimary
                                            subtitlesOn -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        if (state.subtitleTracks.isNotEmpty()) {
                                            Icon(
                                                imageVector = if (subtitlesOn) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                                                contentDescription = "Subtitles",
                                                tint = iconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (state.audioTracks.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Rounded.Audiotrack,
                                                contentDescription = "Audio",
                                                tint = iconTint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSpeedOpen = activePanel == OverlayPanel.SPEED

                            PlayerControlSurface(
                                onClick = {
                                    activePanel = if (isSpeedOpen) OverlayPanel.NONE else OverlayPanel.SPEED
                                },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = if (isSpeedOpen || state.playbackSpeed != 1f) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentDescription = "Playback speed"
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val speedTint = if (isSpeedOpen || state.playbackSpeed != 1f) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.Speed,
                                        contentDescription = "Playback Speed",
                                        tint = speedTint,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${state.playbackSpeed}x",
                                        color = speedTint,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            PlayerControlSurface(
                                onClick = { onResizeModeChanged(resizeMode.next()) },
                                modifier = Modifier.height(44.dp),
                                shape = RoundedCornerShape(22.dp),
                                containerColor = if (resizeMode != VideoResizeMode.FIT) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentDescription = "Toggle video crop mode"
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val cropTint = if (resizeMode != VideoResizeMode.FIT) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = null,
                                        tint = cropTint,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = resizeMode.buttonLabel,
                                        color = cropTint,
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
                                modifier = Modifier.size(44.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentDescription = "Fullscreen"
                            ) {
                                Icon(
                                    imageVector = if (videoIsLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatVideoTime(
                                if (isDragging) (dragProgress * state.duration).toLong() else state.currentPosition
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { newValue ->
                                isDragging = true
                                dragProgress = newValue
                            },
                            onValueChangeFinished = {
                                isDragging = false
                                player.seekTo((dragProgress * state.duration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.35f
                                )
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                        )
                        Text(
                            text = formatVideoTime(state.duration),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(4.dp))


                    with(sharedTransitionScope) {
                        DetailFloatingBar(
                            isFavorite = isFavorite,
                            onActionClick = onActionClick,
                            showContainer = false,
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                zIndexInOverlay = 1f
                            ),
                            allowedActions = DetailAction.entries.filter { it != DetailAction.COPY }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    PlayerControlSurface(
        onClick = onClick,
        shape = CircleShape,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
