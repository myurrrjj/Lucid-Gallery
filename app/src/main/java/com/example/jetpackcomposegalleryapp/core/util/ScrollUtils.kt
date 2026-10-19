package com.example.jetpackcomposegalleryapp.core.util


import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
fun rememberScrollingUp(gridState: LazyGridState): Boolean {
    var isScrollingUp by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(gridState.firstVisibleItemIndex) }
    var previousScrollOffset by remember { mutableIntStateOf(gridState.firstVisibleItemScrollOffset) }

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex == 0 && currentOffset == 0) {
                    isScrollingUp = true
                } else if (currentIndex > previousIndex) {
                    isScrollingUp = false
                } else if (currentIndex < previousIndex) {
                    isScrollingUp = true
                } else {
                    if (currentOffset > previousScrollOffset) {
                        isScrollingUp = false
                    } else if (currentOffset < previousScrollOffset) {
                        isScrollingUp = true
                    }
                }
                previousIndex = currentIndex
                previousScrollOffset = currentOffset
            }
    }

    return !gridState.isScrollInProgress || isScrollingUp
}