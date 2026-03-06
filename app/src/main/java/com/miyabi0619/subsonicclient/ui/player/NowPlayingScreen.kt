package com.miyabi0619.subsonicclient.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miyabi0619.subsonicclient.player.PlaybackState
import com.miyabi0619.subsonicclient.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by playerViewModel.playbackState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("再生中") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = state.currentTitle ?: "—",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = state.currentArtist ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))
            ProgressSection(
                state = state,
                onSeek = { playerViewModel.seekTo(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "前の曲")
                }
                IconButton(onClick = { playerViewModel.playPause() }) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "一時停止" else "再生"
                    )
                }
                IconButton(onClick = { playerViewModel.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "次の曲")
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(
    state: PlaybackState,
    onSeek: (Long) -> Unit
) {
    val durationMs = state.durationMs.coerceAtLeast(0L)
    val safeDuration = durationMs.coerceAtLeast(1L)
    val positionMs = state.positionMs.coerceIn(0L, safeDuration)
    val positionSec = positionMs / 1000
    val durationSec = durationMs / 1000

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..safeDuration.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "%d:%02d".format(positionSec / 60, positionSec % 60),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "%d:%02d".format(durationSec / 60, durationSec % 60),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
