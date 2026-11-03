//package com.example.jetpackcomposegalleryapp.trial
//
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.jetpackcomposegalleryapp.presentation.gallery.EmptyStateView
//import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryEvent
//import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryTab
//import com.example.jetpackcomposegalleryapp.presentation.gallery.components.MediaItemCard
//
//AnimatedContent(
//targetState = state.selectedTab,
//transitionSpec = {
//    fadeIn() + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) togetherWith
//            fadeOut() + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
//},
//label = "TabContent"
//) { tab ->
//    when {
//        state.isLoading -> {
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center),
//                color = MaterialTheme.colorScheme.primary
//            )
//        }
//        !state.hasPermission -> {
//            EmptyStateView(
//                title = "Access Required",
//                description = "To display your beautiful memories, we need access to your device's photos and videos.",
//                buttonText = "Grant Permission",
//                onClick = { permissionLauncher.launch(permissionToRequest) }
//            )
//        }
//        (tab == GalleryTab.ALBUMS && state.albums.isEmpty()) ||
//                (tab != GalleryTab.ALBUMS && state.displayedMediaList.isEmpty()) -> {
//            EmptyStateView(
//                title = if (tab == GalleryTab.ALBUMS) "No Albums Found" else "No Media Found",
//                description = "Your gallery is completely empty. Take some photos to get started!",
//                buttonText = "Refresh",
//                onClick = { viewModel.setEvent(GalleryEvent.LoadMedia) }
//            )
//        }
//        else -> {
//            LazyVerticalGrid(
//                state = gridState,
//                columns = GridCells.Adaptive(minSize = 120.dp),
//                contentPadding = PaddingValues(
//                    top = 0.dp,
//                    bottom = innerPadding.calculateBottomPadding() + 80.dp,
//                    start = 2.dp,
//                    end = 2.dp
//                ),
//                horizontalArrangement = Arrangement.spacedBy(2.dp),
//                verticalArrangement = Arrangement.spacedBy(2.dp),
//                modifier = Modifier.fillMaxSize()
//            ) {
//                if (tab == GalleryTab.ALBUMS) {
//                    items(
//                        count = state.albums.size,
//                        key = { index -> state.albums[index].name ?: "Unknown" }
//                    ) { index ->
//                        with(sharedTransitionScope) {
//                            AlbumCard(
//                                album = state.albums[index],
//                                animatedVisibilityScope = this@AnimatedContent,
//                                onClick = {
//                                    viewModel.setEvent(GalleryEvent.OpenAlbum(state.albums[index]))
//                                },
//                                modifier = Modifier.animateItem()   // ✅ add this
//                            )
//                        }
//                    }
//                } else {
//                    items(
//                        count = state.displayedMediaList.size,
//                        contentType = { index -> if (state.displayedMediaList[index].isVideo) "video" else "photo" },
//                        key = { index -> state.displayedMediaList[index].id }
//                    ) { index ->
//                        val mediaItem = state.displayedMediaList[index]
//                        val isSelected = state.selectedMediaIds.contains(mediaItem.id)
//
//                        MediaItemCard(
//                            media = mediaItem,
//                            isSelected = isSelected,
//                            selectionMode = state.selectionMode,
//                            onClick = {
//                                if (state.selectionMode) {
//                                    viewModel.setEvent(GalleryEvent.ToggleMediaSelection(mediaItem.id))
//                                } else {
//                                    viewModel.setEvent(GalleryEvent.MediaClicked(mediaItem.id))
//                                }
//                            },
//                            onLongClick = {
//                                if (!state.selectionMode) {
//                                    viewModel.setEvent(GalleryEvent.EnterSelectionMode)
//                                    viewModel.setEvent(GalleryEvent.ToggleMediaSelection(mediaItem.id))
//                                }
//                            },
//                            modifier = Modifier.animateItem(),
//                            sharedTransitionScope = sharedTransitionScope,
//                            animatedVisibilityScope = animatedVisibilityScope,
//                        )
//                    }
//                }
//            }
//        }
//    }
//}