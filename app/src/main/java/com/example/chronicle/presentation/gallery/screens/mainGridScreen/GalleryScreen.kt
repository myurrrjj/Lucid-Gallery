package com.example.chronicle.presentation.gallery.screens.mainGridScreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chronicle.core.util.rememberScrollingUp
import com.example.chronicle.domain.model.PersonCluster
import com.example.chronicle.presentation.gallery.GalleryViewModel
import com.example.chronicle.presentation.gallery.contract.GalleryEvent
import com.example.chronicle.presentation.gallery.model.Album
import com.example.chronicle.presentation.gallery.model.GalleryTab
import com.example.chronicle.presentation.gallery.model.OthersSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScreenTarget(
    val album: Album?, val person: PersonCluster?
)


@Suppress("EffectKeys")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    viewModel: GalleryViewModel = hiltViewModel(),
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    val isScrollingUp = rememberScrollingUp(gridState)
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentState = state.contentState
    val configState = state.configState
    val infoSheetState = state.infoSheetState
    val interactionState = state.interactionState

    BackHandler(enabled = interactionState.selectionMode || interactionState.openedAlbum != null || interactionState.openedPersonCluster != null) {
        if (interactionState.selectionMode) {
            viewModel.setEvent(GalleryEvent.ExitSelectionMode)
        } else if (interactionState.openedAlbum != null) {
            viewModel.setEvent(GalleryEvent.CloseAlbum)
        } else if (interactionState.openedPersonCluster != null) {
            viewModel.setEvent(GalleryEvent.ClosePerson)
        }
    }

    val scrollBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val mediaCount = when (interactionState.selectedTab) {
        GalleryTab.ALBUMS -> contentState.albums.size
        else -> contentState.displayedMediaList.size
    }

    LaunchedEffect(Unit) {
        if (contentState.masterMediaList.isEmpty()) {
            viewModel.setEvent(GalleryEvent.LoadMedia)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),

        topBar = {
            GalleryTopAppBar(
                selectionMode = interactionState.selectionMode,
                selectedMediaCount = interactionState.selectedMediaIds.size,
                totalDisplayedCount = contentState.displayedMediaList.size,
                mediaCount = mediaCount,
                scrollBehaviour = scrollBehaviour,
                onEvent = viewModel::setEvent
            )
        },

        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        )
        {
            AnimatedContent(
                targetState = ScreenTarget(
                    interactionState.openedAlbum,
                    interactionState.openedPersonCluster
                ),
                transitionSpec = {
                    val isOpeningDetail = targetState.album != null || targetState.person != null

                    if (isOpeningDetail) {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "GalleryToDetail"
            ) { target ->
                val galleryToDetailScope = this@AnimatedContent

                if (target.album != null || target.person != null) {
                    with(sharedTransitionScope) {
                        GalleryDetailContent(
                            target = target,
                            selectionMode = interactionState.selectionMode,
                            groupedMedia = contentState.groupedMedia,
                            currentViewMode = configState.currentViewMode,
                            selectedMediaIds = interactionState.selectedMediaIds,
                            onEvent = viewModel::setEvent,
                            galleryToDetailScope = galleryToDetailScope,
                            tabAnimatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }


                else {
                    with(sharedTransitionScope){
                        GalleryGridContent(
                            state = state,
                            gridState = gridState,
                            onRequestPermission = onRequestPermission,
                            onEvent = viewModel::setEvent,
                            galleryToDetailScope = galleryToDetailScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }
            }

            val isMainGallery = interactionState.openedAlbum == null && interactionState.openedPersonCluster == null
            val hasContent = if (interactionState.selectedTab == GalleryTab.ALBUMS) {
                contentState.albums.isNotEmpty()
            } else if (interactionState.selectedTab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.PEOPLE) {
                contentState.peopleClusters.isNotEmpty()
            } else {
                contentState.displayedMediaList.isNotEmpty()
            }

            with(sharedTransitionScope) {
                GalleryBottomBar(
                    selectionMode = interactionState.selectionMode,
                    isMainGallery = isMainGallery,
                    hasContent = hasContent,
                    isScrollingUp = isScrollingUp,
                    selectedMediaIds = interactionState.selectedMediaIds,
                    masterMediaList = contentState.masterMediaList,
                    selectedTab = interactionState.selectedTab,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onActionClick = { action, selectedMedia ->
                        viewModel.setEvent(GalleryEvent.PerformMediaAction(action, selectedMedia))
                    },
                    onTabClick = {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    },
                    onTabSelected = { tab ->
                        viewModel.setEvent(GalleryEvent.OnTabSelected(tab))
                        coroutineScope.launch {
                            delay(100)
                            gridState.animateScrollToItem(0)
                        }
                    }
                )
            }
        }
    }
}