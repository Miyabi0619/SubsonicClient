package com.miyabi0619.subsonicclient.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import com.miyabi0619.subsonicclient.data.api.PlaylistDto
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository

@Composable
fun SearchScreen(
    loginRepository: LoginRepository,
    onPlaySong: (songId: String, title: String?, artist: String?, queueIds: List<String>) -> Unit = { _, _, _, _ -> },
    viewModel: SearchViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(loginRepository) as T
            }
        }
    ),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("曲名・アーティスト・アルバム・プレイリストで検索") },
                singleLine = true
            )
            when {
                state.isLoading -> Alignment.CenterHorizontally.let { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.songs.isNotEmpty()) {
                        item {
                            Text(
                                "曲",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        items(state.songs) { song ->
                            SearchSongItem(
                                song = song,
                                onClick = {
                                    val id = song.id ?: return@SearchSongItem
                                    onPlaySong(id, song.title, song.artist, state.songs.mapNotNull { it.id })
                                }
                            )
                        }
                    }
                    if (state.albums.isNotEmpty()) {
                        item {
                            Text(
                                "アルバム",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        items(state.albums) { album ->
                            SearchAlbumItem(album = album)
                        }
                    }
                    if (state.artists.isNotEmpty()) {
                        item {
                            Text(
                                "アーティスト",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        items(state.artists) { artist ->
                            SearchArtistItem(artist = artist)
                        }
                    }
                    if (state.playlists.isNotEmpty()) {
                        item {
                            Text(
                                "プレイリスト",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        items(state.playlists) { playlist ->
                            SearchPlaylistItem(playlist = playlist)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSongItem(song: SongDto, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = song.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${song.artist.orEmpty()} / ${song.album.orEmpty()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SearchAlbumItem(album: AlbumDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = album.title.orEmpty().ifEmpty { album.name.orEmpty() },
                style = MaterialTheme.typography.bodyLarge
            )
            album.artist?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SearchArtistItem(artist: ArtistDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = artist.name.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun SearchPlaylistItem(playlist: PlaylistDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = playlist.name.orEmpty(),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "曲数: ${playlist.songCount ?: 0}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
