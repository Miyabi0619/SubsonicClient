package com.miyabi0619.subsonicclient.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.data.api.AlbumDto
import com.miyabi0619.subsonicclient.data.api.ArtistDto
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository

@Composable
fun HomeScreen(
    loginRepository: LoginRepository,
    onPlaySong: (songId: String, title: String?, artist: String?, queueIds: List<String>) -> Unit = { _, _, _, _ -> },
    viewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(loginRepository) as T
            }
        }
    ),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (state.recentAlbums.isNotEmpty()) {
                    item {
                        Text(
                            "最近追加したアルバム",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.recentAlbums) { album ->
                                AlbumCard(album = album)
                            }
                        }
                    }
                }
                if (state.randomSongs.isNotEmpty()) {
                    item {
                        Text(
                            "おすすめの曲",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.randomSongs) { song ->
                                SongCard(
                                    song = song,
                                    onClick = {
                                        val id = song.id ?: return@SongCard
                                        onPlaySong(id, song.title, song.artist, state.randomSongs.mapNotNull { it.id })
                                    }
                                )
                            }
                        }
                    }
                }
                if (state.artists.isNotEmpty()) {
                    item {
                        Text(
                            "アーティスト",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.artists.take(20)) { artist ->
                                ArtistCard(artist = artist)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: AlbumDto) {
    Card(
        modifier = Modifier.size(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    album.title.orEmpty().ifEmpty { album.name.orEmpty() }.take(1),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Text(
                text = album.title.orEmpty().ifEmpty { album.name.orEmpty() },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun SongCard(song: SongDto, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .height(72.dp)
            .fillMaxWidth(0.85f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    song.title.orEmpty().take(1),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    song.artist.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ArtistCard(artist: ArtistDto) {
    Card(
        modifier = Modifier.size(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    artist.name.orEmpty().take(1),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(
                text = artist.name.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
