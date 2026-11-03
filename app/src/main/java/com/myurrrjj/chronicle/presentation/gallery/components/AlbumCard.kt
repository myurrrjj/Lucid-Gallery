package com.myurrrjj.chronicle.presentation.gallery.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.myurrrjj.chronicle.core.presentation.components.bouncyClick
import com.myurrrjj.chronicle.presentation.gallery.model.Album

@Composable
fun SharedTransitionScope.AlbumCard(
    album: Album,
    onClick: () -> Unit,
    albumAnimatedVisibilityScope: AnimatedVisibilityScope,
    tabAnimatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(album.coverMedia.uriString, album.coverMedia.id) {
        ImageRequest.Builder(context).data(album.coverMedia.uriString).size(300)
            .setParameter("is_thumbnail", true).build()
    }

    Column(
        modifier = modifier
            .bouncyClick { onClick() }
            .padding(4.dp)) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)



        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
//                    .sharedBounds(
//                        sharedContentState = rememberSharedContentState(key = "media_${album.coverMedia.id}"),
//                        animatedVisibilityScope = tabAnimatedVisibilityScope,
//                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
//
//                        )
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "media_local_${album.coverMedia.id}"),
                        animatedVisibilityScope = albumAnimatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(16.dp))
//                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                    )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name ?: "Unknown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "album_title_${album.name}"),
                animatedVisibilityScope = albumAnimatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        )
        Text(
            text = "${album.mediaCount} items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}