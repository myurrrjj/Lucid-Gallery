package com.example.chronicle.presentation.gallery.components.subtitles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer

data class SubtitleState (
    val cues : List<Cue> = emptyList()
)



@Composable
fun rememberSubtitleState(
    player: ExoPlayer
): State<SubtitleState>{
    val state = remember(player){
        mutableStateOf(SubtitleState())
    }
    DisposableEffect(player){
        val listener = object: Player.Listener{
            override fun onCues(cueGroup: CueGroup) {
                state.value = SubtitleState(cues = cueGroup.cues)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }
    return state
}