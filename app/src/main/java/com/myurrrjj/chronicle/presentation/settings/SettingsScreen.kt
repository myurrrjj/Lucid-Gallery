package com.myurrrjj.chronicle.presentation.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.rounded.Face
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.FaceIndexingSchedule
import com.myurrrjj.chronicle.presentation.indexing.FaceIndexingProgressCard
//import com.myurrrjj.chronicle.presentation.indexing.FaceIndexingStartSheet
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myurrrjj.chronicle.core.presentation.components.bouncyClick
import com.myurrrjj.chronicle.core.presentation.indexing.FaceIndexingStartSheet
import com.myurrrjj.chronicle.domain.model.GalleryViewMode

sealed interface SettingItem {
    val title: String
    val icon: ImageVector
    val subtitle: String?

    data class Toggle(
        override val title: String,
        override val icon: ImageVector,
        override val subtitle: String? = null,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingItem

    data class SegmentedOptions<T>(
        override val title: String,
        override val icon: ImageVector,
        override val subtitle: String? = null,
        val options: List<T>,
        val selectedOption: T,
        val onOptionSelected: (T) -> Unit,
        val optionLabel: (T) -> String
    ) : SettingItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenFaceLab: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val settingsList = remember(state.appSettings) {
        listOf(
            SettingItem.Toggle(
            title = "Auto-Play Videos",
            subtitle = "Play videos automatically when swiping through the gallery",
            icon = Icons.Rounded.PlayCircle,
            isChecked = state.appSettings.autoPlayVideo,
            onToggle = { enabled ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setEvent(SettingsEvent.ToggleAutoPlay(enabled))
            }),
            SettingItem.SegmentedOptions(
                title = "Default View Mode",
                subtitle = "Choose how your gallery grid looks when the app opens",
                icon = Icons.Rounded.ViewDay,
                options = GalleryViewMode.entries,
                selectedOption = state.appSettings.defaultGalleryViewMode,
                onOptionSelected = { mode ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setEvent(SettingsEvent.ChangeDefaultViewMode(mode))
                },
                optionLabel = {
                    it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
                }),
            SettingItem.SegmentedOptions(
                title = "Face indexing schedule",
                subtitle = "When Chronicle is allowed to scan for faces",
                icon = Icons.Rounded.Face,
                options = FaceIndexingSchedule.entries,
                selectedOption = state.appSettings.faceIndexingOptions.schedule,
                onOptionSelected = { schedule ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setEvent(
                        SettingsEvent.ChangeFaceIndexingOptions(
                            state.appSettings.faceIndexingOptions.copy(schedule = schedule)
                        )
                    )
                },
                optionLabel = { it.label }),
            SettingItem.Toggle(
                title = "Pause when battery is low",
                subtitle = "Stops scanning below 15% and resumes when you charge",
                icon = Icons.Rounded.Face,
                isChecked = state.appSettings.faceIndexingOptions.pauseOnLowBattery,
                onToggle = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setEvent(
                        SettingsEvent.ChangeFaceIndexingOptions(
                            state.appSettings.faceIndexingOptions.copy(pauseOnLowBattery = enabled)
                        )
                    )
                }),
            SettingItem.Toggle(
                title = "Pause when storage is low",
                subtitle = "Stops scanning if the device runs short on space",
                icon = Icons.Rounded.Face,
                isChecked = state.appSettings.faceIndexingOptions.pauseOnLowStorage,
                onToggle = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setEvent(
                        SettingsEvent.ChangeFaceIndexingOptions(
                            state.appSettings.faceIndexingOptions.copy(pauseOnLowStorage = enabled)
                        )
                    )
                }))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }, colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
            )
        }, containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AnimatedContent(
            targetState = state.isLoading,
            label = "SettingsContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { isLoading ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (state.faceIndexing.isVisible) {
                        item(key = "face_indexing_progress") {
                            FaceIndexingProgressCard(
                                progress = state.faceIndexing,
                                onPause = {
                                    viewModel.setEvent(SettingsEvent.PauseFaceIndexing)
                                },
                                onResume = {
                                    viewModel.setEvent(SettingsEvent.ResumeFaceIndexing)
                                },
                                onForceStop = {
                                    viewModel.setEvent(SettingsEvent.ForceStopFaceIndexing)
                                },
                                onReset = {
                                    viewModel.setEvent(SettingsEvent.ResetFaceIndexing)
                                }
                            )
                        }
                    }
                    items(settingsList, key = { it.title }) { item ->
                        when (item) {
                            is SettingItem.Toggle -> SettingsToggleCard(item)
                            is SettingItem.SegmentedOptions<*> -> SettingsSegmentedCard(item)
                        }
                    }
//                    item {
//                        Button(
//                            onClick = onOpenFaceLab, modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text("Developer Tools — Face Quality Lab")
//                        }
//                    }
                    item(key = "face_indexing_action") {
                        Button(
                            onClick = {
                                if (state.faceIndexing.canResume) {
                                    viewModel.setEvent(SettingsEvent.ResumeFaceIndexing)
                                } else {
                                    viewModel.setEvent(SettingsEvent.OpenFaceIndexingSheet)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.faceIndexing.canStart ||
                                    state.faceIndexing.canResume ||
                                    state.faceIndexing.status ==
                                    com.myurrrjj.chronicle.domain.model.FaceIndexingStatus.PAUSED
                        ) {
                            Text(
                                when {
                                    state.faceIndexing.canResume -> "Resume face indexing"
                                    state.faceIndexing.isBusy -> "Indexing in progress…"
                                    state.faceIndexing.canStart -> "Start face indexing"
                                    else -> "All photos indexed"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (state.showFaceIndexingSheet) {
        FaceIndexingStartSheet(
            initialOptions = state.appSettings.faceIndexingOptions,
            progress = state.faceIndexing,
            onDismiss = { viewModel.setEvent(SettingsEvent.DismissFaceIndexingSheet) },
            onStart = { options ->
                viewModel.setEvent(SettingsEvent.StartFaceIndexing(options))
            }
        )
    }
}

@Composable
private fun SettingsToggleCard(item: SettingItem.Toggle) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick { item.onToggle(!item.isChecked) }) {
        Row(
            modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                item.subtitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = item.isChecked, onCheckedChange = null
            )
        }
    }
}

@Composable
private fun <T> SettingsSegmentedCard(item: SettingItem.SegmentedOptions<T>) {
    val useTwoRows = item.options.size > 3

    Surface(
        shape =  RoundedCornerShape(24.dp) ,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    item.subtitle?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = if (useTwoRows) RoundedCornerShape(24.dp) else CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.options
                        .chunked(
                            if (useTwoRows) 2
                            else item.options.size.coerceAtLeast(1)
                        )
                        .forEach { rowOptions ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowOptions.forEach { option ->
                                    val isSelected =
                                        item.selectedOption == option

                                    val containerColor by animateColorAsState(
                                        targetValue = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.Transparent,
                                        label = "SegmentColor"
                                    )

                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        label = "SegmentContentColor"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 68.dp)
                                            .clip(CircleShape)
                                            .background(containerColor)
                                            .bouncyClick {
                                                item.onOptionSelected(option)
                                            }
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = 12.dp
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.optionLabel(option),
                                            color = contentColor,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                if (useTwoRows && rowOptions.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                }
            }
        }
    }
}