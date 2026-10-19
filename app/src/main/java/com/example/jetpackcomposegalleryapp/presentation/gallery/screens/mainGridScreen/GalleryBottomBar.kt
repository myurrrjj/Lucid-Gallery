package com.example.jetpackcomposegalleryapp.presentation.gallery.screens.mainGridScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailAction
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.DetailFloatingBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.FloatingGalleryBar
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import kotlinx.coroutines.Job

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.GalleryBottomBar(
    selectionMode: Boolean,
    isMainGallery: Boolean,
    hasContent: Boolean,
    isScrollingUp: Boolean,
    selectedMediaIds: Set<Long>,
    masterMediaList: List<MediaAsset>,
    selectedTab: GalleryTab,
    animatedVisibilityScope: AnimatedContentScope,
    onActionClick: (DetailAction, List<MediaAsset>) -> Unit,
    onTabClick: () -> Job,
    onTabSelected: (GalleryTab) -> Unit
) {
    val shouldShowBar by remember(hasContent, isScrollingUp) {
        derivedStateOf { !hasContent || isScrollingUp }
    }
    val bottomBarVisible = selectionMode || (isMainGallery && shouldShowBar)

    val barOffset by animateDpAsState(
        targetValue = if (bottomBarVisible) 0.dp else 150.dp, label = "barOffset"
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (bottomBarVisible) 1f else 0f, label = "barAlpha"
    )

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .renderInSharedTransitionScopeOverlay(2f)

            .padding(
                bottom = WindowInsets.systemBars.asPaddingValues()
                    .calculateBottomPadding() + 24.dp
            )
            .graphicsLayer {
                translationY = barOffset.toPx()
                alpha = barAlpha
            }
            .zIndex(1f)) {
        AnimatedContent(
            targetState = selectionMode, label = "BottomBarSwap"
        ) { isSelectionMode ->
            if (isSelectionMode) {
                val selectedMedia = masterMediaList.filter { it.id in selectedMediaIds }
                DetailFloatingBar(
                    isFavorite = false, onActionClick = { action ->
                        onActionClick(action, selectedMedia)
                    }, allowedActions = listOfNotNull(
                        DetailAction.SHARE,
                        DetailAction.FAVOURITE,
                        DetailAction.DELETE,
                        if (selectedMedia.none { it.isVideo }) {
                            DetailAction.COPY
                        } else null
                    )
                )
            } else if (isMainGallery) {
                FloatingGalleryBar(
                    selectedTab = selectedTab,
                    onClick = onTabClick,
                    onTabSelected = onTabSelected,
                    modifier = Modifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "floating_navigation_bar"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        zIndexInOverlay = 1f
                    )
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}