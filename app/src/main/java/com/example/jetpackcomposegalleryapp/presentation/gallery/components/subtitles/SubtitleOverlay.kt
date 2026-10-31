package com.example.jetpackcomposegalleryapp.presentation.gallery.components.subtitles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubtitleOverlay(
    state: SubtitleState,
    fontSize : TextUnit = 18.sp,
    verticalPosition: Float,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .align(
                    BiasAlignment(
                        horizontalBias = 0f,
                        verticalBias = verticalPosition * 2f - 1f
                            .coerceIn(-.9f,.9f)
                    )
                )
                .fillMaxWidth()
                .padding(horizontal = 20.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            state.cues.forEach { cue ->

                val text = cue.text?.toString()

                if (!text.isNullOrBlank()) {

                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                    )
                }
            }
        }
    }
}