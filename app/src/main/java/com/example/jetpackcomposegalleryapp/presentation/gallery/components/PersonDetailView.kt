package com.example.jetpackcomposegalleryapp.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.computeHorizontalBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.jetpackcomposegalleryapp.domain.model.GalleryViewMode
import com.example.jetpackcomposegalleryapp.domain.model.MediaAsset
import com.example.jetpackcomposegalleryapp.domain.model.PersonCluster
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun SharedTransitionScope.PersonDetailView(
    cluster: PersonCluster,
    groupedMedia: ImmutableMap<String, ImmutableList<MediaAsset>>,
    currentViewMode: GalleryViewMode,
    selectedMediaIds: ImmutableSet<Long> = persistentSetOf(),
    selectionMode: Boolean = false,
    onToggleSelection: (Long) -> Unit,
    onEnterSelectionMode: () -> Unit,
    onViewModeChanged: (GalleryViewMode) -> Unit,
    onUpdateName: (Long, String) -> Unit,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    personAnimatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onMediaClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editNameValue by remember(cluster.name) { mutableStateOf(cluster.name) }
    val gridState = rememberLazyGridState()

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Rename Person") },
            text = {
                OutlinedTextField(
                    value = editNameValue,
                    onValueChange = { editNameValue = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameValue.isNotBlank()) {
                            onUpdateName(cluster.id, editNameValue.trim())
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = cluster.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "person_title_${cluster.id}"),
                        animatedVisibilityScope = personAnimatedVisibilityScope,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                    )
            )
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Name",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        UniversalMediaGrid(
            groupedMedia = groupedMedia,
            currentViewMode = currentViewMode,
            gridState = gridState,
            selectionMode = selectionMode,
            selectedMediaIds = selectedMediaIds,
            onMediaClick = onMediaClick,
            onToggleSelection = onToggleSelection,
            onEnterSelectionMode = onEnterSelectionMode,
            onViewModeChanged = onViewModeChanged,
            sharedTransitionScope = this@PersonDetailView,
            tabAnimatedVisibilityScope = tabAnimatedVisibilityScope,
            albumAnimatedVisibilityScope = personAnimatedVisibilityScope,
            modifier = Modifier.weight(1f)
        )
    }
}