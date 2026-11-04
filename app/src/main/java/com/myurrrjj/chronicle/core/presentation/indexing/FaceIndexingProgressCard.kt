package com.myurrrjj.chronicle.presentation.indexing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myurrrjj.chronicle.domain.model.FaceIndexingProgress
import com.myurrrjj.chronicle.domain.model.FaceIndexingStatus
import java.text.NumberFormat

@Composable
fun FaceIndexingProgressCard(
    progress: FaceIndexingProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onForceStop: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!progress.isVisible) return

    var showResetDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    val numberFormat = remember { NumberFormat.getIntegerInstance() }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset face index?") },
            text = {
                Text(
                    "This deletes every face, every detected person, and every " + "saved face thumbnail. Your photos are not touched. " + "Indexing will have to start again from zero."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            })
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Stop immediately?") },
            text = {
                Text(
                    "Pause is the safe option — it finishes the current batch first. " + "Stopping now throws away the batch in progress. Nothing is " + "corrupted, but those photos will be scanned again later."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    onForceStop()
                }) {
                    Text("Stop now", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) { Text("Keep going") }
            })
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusTitle(progress),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${progress.percentage}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progress.total <= 0 || progress.status == FaceIndexingStatus.QUEUED) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = buildString {
                    append(numberFormat.format(progress.processed))
                    append(" of ")
                    append(numberFormat.format(progress.total))
                    append(" photos")
                    if (progress.facesFound > 0) {
                        append(" · ")
                        append(numberFormat.format(progress.facesFound))
                        append(" faces found")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val subtitle = statusSubtitle(progress)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (progress.canPause) {
                    TextButton(onClick = onPause) { Text("Pause") }
                }
                if (progress.canResume) {
                    TextButton(onClick = onResume) { Text("Resume") }
                }
                if (progress.isBusy) {
                    TextButton(onClick = { showStopDialog = true }) {
                        Text("Stop now", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun statusTitle(progress: FaceIndexingProgress): String = when (progress.status) {
    FaceIndexingStatus.IDLE -> "Face indexing"
    FaceIndexingStatus.QUEUED -> "Waiting to start"
    FaceIndexingStatus.RUNNING -> "Finding people…"
    FaceIndexingStatus.PAUSING -> "Pausing…"
    FaceIndexingStatus.PAUSED -> "Paused"
    FaceIndexingStatus.COMPLETED -> "All photos indexed"
    FaceIndexingStatus.FAILED -> "Indexing stopped"
}

private fun statusSubtitle(progress: FaceIndexingProgress): String? = when (progress.status) {
    FaceIndexingStatus.RUNNING -> progress.currentItem?.let { "Scanning $it" }
    FaceIndexingStatus.PAUSING -> progress.message ?: "Finishing the current batch…"
    FaceIndexingStatus.PAUSED -> "${progress.pending} photos left. Resume whenever you like — nothing is lost."
    FaceIndexingStatus.QUEUED -> progress.message ?: "Waiting for the device to be ready"
    FaceIndexingStatus.COMPLETED -> null
    FaceIndexingStatus.FAILED -> progress.message ?: "Tap Resume to continue."
    FaceIndexingStatus.IDLE -> null
}