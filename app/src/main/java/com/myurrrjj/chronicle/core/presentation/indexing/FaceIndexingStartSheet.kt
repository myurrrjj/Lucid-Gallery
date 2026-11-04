package com.myurrrjj.chronicle.core.presentation.indexing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myurrrjj.chronicle.domain.model.FaceIndexingOptions
import com.myurrrjj.chronicle.domain.model.FaceIndexingProgress
import com.myurrrjj.chronicle.domain.model.FaceIndexingSchedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceIndexingStartSheet(
    initialOptions: FaceIndexingOptions,
    progress: FaceIndexingProgress,
    onDismiss: () -> Unit,
    onStart: (FaceIndexingOptions) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var schedule by remember(initialOptions) { mutableStateOf(initialOptions.schedule) }
    var pauseOnLowBattery by remember(initialOptions) {
        mutableStateOf(initialOptions.pauseOnLowBattery)
    }
    var pauseOnLowStorage by remember(initialOptions) {
        mutableStateOf(initialOptions.pauseOnLowStorage)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (progress.canResume) "Resume indexing" else "Find people in your photos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Chronicle looks for faces and groups them by person. " + "Everything happens on this device — no photo ever leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (progress.total > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (progress.pending > 0) {
                        "${progress.pending} of ${progress.total} photos still to scan."
                    } else {
                        "All ${progress.total} photos are already indexed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "When should this run?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FaceIndexingSchedule.entries.forEach { option ->
                ScheduleOptionRow(
                    option = option, selected = schedule == option, onClick = { schedule = option })
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pause automatically",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            OptionSwitchRow(
                title = "When the battery is low",
                subtitle = "Stops scanning below 15% and resumes when you charge.",
                checked = pauseOnLowBattery,
                onCheckedChange = { pauseOnLowBattery = it })

            OptionSwitchRow(
                title = "When storage is low",
                subtitle = "Stops scanning if the device is short on space.",
                checked = pauseOnLowStorage,
                onCheckedChange = { pauseOnLowStorage = it })

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onStart(
                        FaceIndexingOptions(
                            schedule = schedule,
                            pauseOnLowBattery = pauseOnLowBattery,
                            pauseOnLowStorage = pauseOnLowStorage
                        )
                    )
                }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (progress.canResume) "Resume indexing" else "Start indexing",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onDismiss, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not now")
            }
        }
    }
}

@Composable
private fun ScheduleOptionRow(
    option: FaceIndexingSchedule, selected: Boolean, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Transparent)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.dp)
    )
}

@Composable
private fun OptionSwitchRow(
    title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}