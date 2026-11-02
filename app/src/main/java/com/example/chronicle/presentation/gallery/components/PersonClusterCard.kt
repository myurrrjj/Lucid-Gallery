package com.example.chronicle.presentation.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.chronicle.core.presentation.components.bouncyClick
import com.example.chronicle.domain.model.MediaAsset
import com.example.chronicle.domain.model.PersonCluster
import java.io.File

@Composable
fun SharedTransitionScope.PersonClusterCard(
    cluster: PersonCluster,
    coverMedia: MediaAsset?,
    onClick: () -> Unit,
    personAnimatedVisibilityScope: AnimatedVisibilityScope,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cropFile = remember(cluster.coverEmbeddingId) {
        File(context.filesDir, "face_crops/${cluster.coverEmbeddingId}.jpg")
    }
    val imageRequest = remember(cropFile) {
        ImageRequest.Builder(context)
            .data(cropFile)
            .crossfade(true)
            .build()
    }

    Column(
        modifier = modifier
            .bouncyClick { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "person_media_${cluster.id}"),
                    animatedVisibilityScope = tabAnimatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                )
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "person_local_${cluster.id}"),
                    animatedVisibilityScope = personAnimatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                )
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = cluster.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = cluster.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = "person_title_${cluster.id}"),
                animatedVisibilityScope = personAnimatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        )
    }
}