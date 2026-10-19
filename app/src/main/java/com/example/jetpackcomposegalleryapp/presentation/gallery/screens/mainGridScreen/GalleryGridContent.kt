package com.example.jetpackcomposegalleryapp.presentation.gallery.screens.mainGridScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.core.presentation.components.bouncyClick
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.AlbumCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.EmptyStateView
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.PersonClusterCard
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.UniversalMediaGrid
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryState
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.GalleryTab
import com.example.jetpackcomposegalleryapp.presentation.gallery.model.OthersSelection

@Composable
fun SharedTransitionScope.GalleryGridContent(
    state: GalleryState,
    gridState: LazyGridState,
    onRequestPermission: () -> Unit,
    onEvent: (GalleryEvent) -> Unit,
    galleryToDetailScope: AnimatedContentScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    val contentState = (state).contentState
    val configState = (state).configState
    val interactionState = (state).interactionState
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = interactionState.selectedTab,

            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                            (slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "TabContent"
        ) { tab ->
            when {
                configState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                !configState.hasPermission -> {
                    EmptyStateView(
                        title = "Access Required",
                        description = "To display your beautiful memories, we need access to your device's photos and videos.",
                        buttonText = "Grant Permission",
                        onClick = onRequestPermission
                    )
                }

                (tab == GalleryTab.ALBUMS && contentState.albums.isEmpty()) || (tab != GalleryTab.ALBUMS && tab != GalleryTab.OTHERS && contentState.displayedMediaList.isEmpty()) -> {
                    EmptyStateView(
                        title = "No Content Found",
                        description = "Your gallery section is empty. Take some photos to get started!",
                        buttonText = "Refresh",
                        onClick = { onEvent(GalleryEvent.LoadMedia) })
                }

                else -> {
                    val isPeopleTab =
                        tab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.PEOPLE
                    val isFavouritesTab =
                        tab == GalleryTab.OTHERS && interactionState.othersSelection == OthersSelection.FAVOURITES

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (tab == GalleryTab.OTHERS) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OthersSelection.entries.forEach { selection ->
                                            val isSelected =
                                                interactionState.othersSelection == selection
                                            val containerColor by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                label = "SegmentContainerColor"
                                            )
                                            val contentColor by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                label = "SegmentContentColor"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(containerColor)
                                                    .bouncyClick {
                                                        onEvent(
                                                            GalleryEvent.OnOthersSelectionChanged(
                                                                selection
                                                            )
                                                        )
                                                    }
                                                    .padding(horizontal = 24.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = selection.title,
                                                    color = contentColor,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val isOthersEmpty = tab == GalleryTab.OTHERS &&
                                ((isPeopleTab && contentState.peopleClusters.isEmpty()) ||
                                        (isFavouritesTab && contentState.displayedMediaList.isEmpty()))

                        if (isOthersEmpty) {
                            EmptyStateView(
                                title = if (isPeopleTab) "Find People" else "No Favourites",
                                description = if (isPeopleTab) "Plug in your device to scan your gallery for faces." else "No items found.",
                                buttonText = if (isPeopleTab) "Start Indexing" else "Refresh",
                                onClick = {
                                    if (isPeopleTab) onEvent(GalleryEvent.StartFaceIndexing)
                                    else onEvent(GalleryEvent.LoadMedia)
                                },
                                modifier = Modifier.padding(top = 64.dp)
                            )
                        } else {
                            when {
                                tab == GalleryTab.ALBUMS -> {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = 150.dp),
                                        contentPadding = PaddingValues(
                                            top = 8.dp, start = 2.dp, end = 2.dp,
                                            bottom = WindowInsets.systemBars.asPaddingValues()
                                                .calculateBottomPadding() + 100.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            count = contentState.albums.size,
                                            key = { index ->
                                                contentState.albums[index].name ?: "Unknown"
                                            }
                                        ) { index ->
                                            AlbumCard(
                                                album = contentState.albums[index],
                                                albumAnimatedVisibilityScope = galleryToDetailScope,
                                                tabAnimatedVisibilityScope = animatedVisibilityScope,
                                                onClick = {
                                                    onEvent(
                                                        GalleryEvent.OpenAlbum(
                                                            contentState.albums[index]
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }

                                isPeopleTab -> {
                                    LazyVerticalGrid(
                                        state = gridState,
                                        columns = GridCells.Adaptive(minSize = 140.dp),
                                        contentPadding = PaddingValues(
                                            top = 8.dp, start = 2.dp, end = 2.dp,
                                            bottom = WindowInsets.systemBars.asPaddingValues()
                                                .calculateBottomPadding() + 100.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            count = contentState.peopleClusters.size,
                                            key = { index -> contentState.peopleClusters[index].id }
                                        ) { index ->
                                            val cluster = contentState.peopleClusters[index]
                                            val coverMedia =
                                                contentState.masterMediaList.find { it.id == cluster.coverMediaId }
                                            PersonClusterCard(
                                                cluster = cluster,
                                                coverMedia = coverMedia,
                                                onClick = { onEvent(GalleryEvent.OpenPerson(cluster)) },
                                                personAnimatedVisibilityScope = galleryToDetailScope,
                                                tabAnimatedVisibilityScope = animatedVisibilityScope,
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    UniversalMediaGrid(
                                        groupedMedia = contentState.groupedMedia,
                                        currentViewMode = configState.currentViewMode,
                                        gridState = gridState,
                                        selectionMode = interactionState.selectionMode,
                                        selectedMediaIds = interactionState.selectedMediaIds,
                                        onMediaClick = { onEvent(GalleryEvent.MediaClicked(it)) },
                                        onToggleSelection = {
                                            onEvent(
                                                GalleryEvent.ToggleMediaSelection(
                                                    it
                                                )
                                            )
                                        },
                                        onEnterSelectionMode = { onEvent(GalleryEvent.EnterSelectionMode) },
                                        onViewModeChanged = { onEvent(GalleryEvent.ChangeViewMode(it)) },
                                        sharedTransitionScope = this@GalleryGridContent,
                                        tabAnimatedVisibilityScope = animatedVisibilityScope,
                                        albumAnimatedVisibilityScope = galleryToDetailScope,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}