package com.miyabi0619.subsonicclient.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.data.api.SubsonicCoverArtUrlBuilder
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import com.miyabi0619.subsonicclient.player.PlaybackState
import com.miyabi0619.subsonicclient.player.PlayerViewModel
import com.miyabi0619.subsonicclient.ui.common.CoverArtImage

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel,
    loginRepository: LoginRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by playerViewModel.playbackState.collectAsState()
    val creds by loginRepository.credentials.collectAsState(initial = null)

    val lyricsViewModel: LyricsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LyricsViewModel(loginRepository) as T
            }
        }
    )
    val lyricsState by lyricsViewModel.uiState.collectAsState()

    LaunchedEffect(state.currentTitle, state.currentArtist) {
        lyricsViewModel.loadLyrics(state.currentArtist, state.currentTitle)
    }

    val coverArtUrl = remember(state.currentSongId, creds) {
        creds?.let {
            SubsonicCoverArtUrlBuilder.build(
                serverUrl = it.serverUrl,
                username = it.username,
                password = it.password,
                coverArtId = state.currentSongId,
                size = 600
            )
        }
    }

    // page 0 = detail (default), page 1 = lyrics (left swipe from detail)
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    Column(modifier = modifier.fillMaxSize()) {
        // 常に表示するヘッダー行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
            }
            Text(
                text = if (pagerState.currentPage == 1) "歌詞" else "再生中",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> DetailPage(
                    state = state,
                    coverArtUrl = coverArtUrl,
                    onSeek = { playerViewModel.seekTo(it) },
                    onPlayPause = { playerViewModel.playPause() },
                    onPrevious = { playerViewModel.seekToPrevious() },
                    onNext = { playerViewModel.seekToNext() },
                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                    onCycleRepeat = { playerViewModel.cycleRepeatMode() }
                )
                else -> LyricsPage(lyricsState = lyricsState)
            }
        }
    }
}

@Composable
private fun DetailPage(
    state: PlaybackState,
    coverArtUrl: String?,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // アルバムアート
        CoverArtImage(
            url = coverArtUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 16.dp),
            contentDescription = state.currentTitle,
            placeholder = {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 曲名・アーティスト
        Text(
            text = state.currentTitle ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.currentArtist ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        // プログレスバー
        ProgressSection(state = state, onSeek = onSeek)

        Spacer(modifier = Modifier.height(8.dp))

        // 再生コントロール
        val activeTint = MaterialTheme.colorScheme.primary
        val inactiveTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "シャッフル",
                    tint = if (state.shuffleEnabled) activeTint else inactiveTint
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "前の曲")
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "一時停止" else "再生"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "次の曲")
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (state.repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "リピート",
                    tint = if (state.repeatMode != 0) activeTint else inactiveTint
                )
            }
        }
    }
}

@Composable
private fun LyricsPage(
    lyricsState: LyricsUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyricsState.isLoading -> CircularProgressIndicator()
            lyricsState.isUnavailable -> Text(
                "歌詞が見つかりませんでした",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            lyricsState.lyrics != null -> Text(
                text = lyricsState.lyrics,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
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
