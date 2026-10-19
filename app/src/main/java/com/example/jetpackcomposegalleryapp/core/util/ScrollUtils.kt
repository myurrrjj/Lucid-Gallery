package com.example.jetpackcomposegalleryapp.core.util

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.State

@Composable
fun rememberScrollingUp(gridState: LazyGridState): State<Boolean> {
    val isScrollingUp = remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(gridState.firstVisibleItemIndex) }
    var previousScrollOffset by remember { mutableIntStateOf(gridState.firstVisibleItemScrollOffset) }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                isScrollingUp.value = when {
                    currentIndex == 0 && currentOffset == 0 -> true
                    currentIndex > previousIndex -> false
                    currentIndex < previousIndex -> true
                    currentOffset > previousScrollOffset -> false
                    currentOffset < previousScrollOffset -> true
                    else -> isScrollingUp.value
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    return remember(gridState) {
        derivedStateOf { !gridState.isScrollInProgress || isScrollingUp.value }
    }
}
