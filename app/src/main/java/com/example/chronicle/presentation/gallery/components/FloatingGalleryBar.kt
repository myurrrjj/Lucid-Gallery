package com.example.chronicle.presentation.gallery.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chronicle.core.presentation.components.bouncyClick
import com.example.chronicle.presentation.gallery.model.GalleryTab
import kotlinx.coroutines.Job


@Composable
fun FloatingGalleryBar(
    selectedTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Job
) {
    Row(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = .25f)

            )
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)

    ) {
        GalleryTab.entries.forEachIndexed { index, tab ->
            val isSelected = selectedTab == tab
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                animationSpec = tween(durationMillis = 300),
                label = "TabColorAnimation"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "TabTextColorAnimation"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(containerColor)
                    .bouncyClick {
                        onTabSelected(tab)
                        if (selectedTab == tab) {
                            onClick()
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = tab.title,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}