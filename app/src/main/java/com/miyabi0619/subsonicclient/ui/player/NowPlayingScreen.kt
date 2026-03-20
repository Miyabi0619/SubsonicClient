package com.miyabi0619.subsonicclient.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.currentTitle ?: "—",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = state.currentArtist ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProgressSection(
                    state = state,
                    onSeek = { playerViewModel.seekTo(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeTint = MaterialTheme.colorScheme.primary
                    val inactiveTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                    IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "シャッフル",
                            tint = if (state.shuffleEnabled) activeTint else inactiveTint
                        )
                    }
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
                    IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {
                        Icon(
                            imageVector = if (state.repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "リピート",
                            tint = if (state.repeatMode != 0) activeTint else inactiveTint
                        )
                    }
                }
            }

            if (state.queueSize > 1) {
                HorizontalDivider()
                QueueSection(
                    queueSize = state.queueSize,
                    queueIndex = state.queueIndex,
                    onItemClick = { index -> playerViewModel.seekToMediaItem(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QueueSection(
    queueSize: Int,
    queueIndex: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(queueIndex) {
        if (queueIndex >= 0 && queueIndex < queueSize) {
            listState.animateScrollToItem(queueIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = "再生キュー  ${queueIndex + 1} / $queueSize",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(List(queueSize) { it }) { index, _ ->
                val isCurrent = index == queueIndex
                ListItem(
                    headlineContent = {
                        Text(
                            text = "曲 ${index + 1}",
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.clickable { onItemClick(index) },
                    colors = if (isCurrent) {
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        ListItemDefaults.colors()
                    }
                )
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
