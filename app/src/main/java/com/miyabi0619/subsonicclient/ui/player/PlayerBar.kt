package com.miyabi0619.subsonicclient.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miyabi0619.subsonicclient.player.PlaybackState
import com.miyabi0619.subsonicclient.player.PlayerViewModel

@Composable
fun PlayerBar(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!playbackState.hasController) return
    if (!playbackState.isReady && playbackState.currentTitle.isNullOrBlank()) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { },
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playbackState.currentTitle ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = playbackState.currentArtist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
            ) {
                if (playbackState.isPlaying) {
                    Text("⏸", style = MaterialTheme.typography.titleLarge)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "再生"
                    )
                }
            }
        }
    }
}
