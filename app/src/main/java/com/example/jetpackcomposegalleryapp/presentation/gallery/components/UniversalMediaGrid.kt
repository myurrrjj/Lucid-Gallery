package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet

@Composable
fun UniversalMediaGrid(
    groupedMedia: ImmutableMap<String, ImmutableList<MediaAsset>>,
    currentViewMode: GalleryViewMode,
    gridState: LazyGridState,
    selectionMode: Boolean,
    selectedMediaIds: ImmutableSet<Long>,
    onMediaClick: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onViewModeChanged: (GalleryViewMode) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    albumAnimatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = currentViewMode.minCellSizeDp.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            start = 2.dp,
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 100.dp,
            end = 2.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxSize()
            .galleryZoomGesture(
                currentMode = currentViewMode,
                onViewModeChanged = onViewModeChanged
            )
    ) {
        groupedMedia.forEach { (groupKey, mediaItems) ->
            if (mediaItems.isNotEmpty()) {
                item(
                    key = "header_$groupKey",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "contentType1"
                ) {
                    Text(
                        text = currentViewMode.formatHeader(mediaItems.first().dateAdded),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 24.dp)
                            .animateItem()
                    )
                }
                items(
                    count = mediaItems.size,
                    contentType = { index -> if (mediaItems[index].isVideo) "video" else "photo" },
                    key = { index -> mediaItems[index].id }
                ) { index ->
                    val mediaItem = mediaItems[index]
                    val isSelected = selectedMediaIds.contains(mediaItem.id)

                    with(sharedTransitionScope) {
                        MediaItemCard(
                            size = when (currentViewMode) {
                                GalleryViewMode.DAY -> 400
                                GalleryViewMode.MONTH -> 150
                                GalleryViewMode.YEAR -> 20
                            },
                            media = mediaItem,
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    onToggleSelection(mediaItem.id)
                                } else {
                                    onMediaClick(mediaItem.id)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    onEnterSelectionMode()
                                    onToggleSelection(mediaItem.id)
                                }
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            tabAnimatedVisibilityScope = tabAnimatedVisibilityScope,
                            albumAnimatedVisibilityScope = albumAnimatedVisibilityScope,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}