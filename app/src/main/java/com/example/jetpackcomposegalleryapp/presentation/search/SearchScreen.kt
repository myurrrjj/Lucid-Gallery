package com.example.jetpackcomposegalleryapp.presentation.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.persistentSetOf
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetpackcomposegalleryapp.presentation.gallery.GalleryViewModel
import com.example.jetpackcomposegalleryapp.presentation.gallery.components.UniversalMediaGrid
import com.example.jetpackcomposegalleryapp.presentation.gallery.contract.GalleryEvent
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchEffect
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchEvent
import com.example.jetpackcomposegalleryapp.presentation.search.contract.SearchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    galleryViewModel: GalleryViewModel,
    sharedTransitionScope: SharedTransitionScope
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToDetail -> {
                    val flatResults = viewModel.uiState.value.groupedResults.values.flatten()
                    galleryViewModel.setEvent(GalleryEvent.SetSearchMediaList(flatResults))
                    onNavigateToDetail(effect.mediaId)
                }

                SearchEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = state.isIndexing,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "SearchScreenStateTransition"
        ) { isIndexing ->
            if (isIndexing) {
                IndexingOverlay(progress = state.indexingProgress)
            } else {
                ActiveSearchContent(
                    animatedVisibilityScope = animatedVisibilityScope,
                    state = state,
                    sharedTransitionScope = sharedTransitionScope,
                    onEvent = viewModel::setEvent,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}

@Composable
private fun IndexingOverlay(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IndexingProgressAnimation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Indexing",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Preparing Smart Search...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organizing your memories for lightning-fast results.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(8.dp)
                .clip(RoundedCornerShape(100)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSearchContent(
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
) {
    val gridState = rememberLazyGridState()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            onQueryChange = { onEvent(SearchEvent.OnQueryChanged(it)) },
            onSearch = { /* Handled automatically by the debounce flow */ },
            active = true,
            onActiveChange = { if (!it) onNavigateBack() },
            placeholder = { Text("Search photos, videos, dates...") },
            leadingIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onEvent(SearchEvent.ClearSearch) }) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isSearching,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                UniversalMediaGrid(
                    groupedMedia = state.groupedResults,
                    currentViewMode = state.currentViewMode,
                    gridState = gridState,
                    selectionMode = false,
                    selectedMediaIds = persistentSetOf(),
                    onMediaClick = { onEvent(SearchEvent.OnMediaClicked(it)) },
                    onToggleSelection = { },
                    onEnterSelectionMode = { },
                    onViewModeChanged = { onEvent(SearchEvent.ChangeViewMode(it)) },
                    sharedTransitionScope = sharedTransitionScope,
                    tabAnimatedVisibilityScope = animatedVisibilityScope,
                    albumAnimatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}