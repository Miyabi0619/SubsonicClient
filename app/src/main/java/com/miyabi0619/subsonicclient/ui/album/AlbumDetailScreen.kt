package com.miyabi0619.subsonicclient.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.api.SubsonicCoverArtUrlBuilder
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import com.miyabi0619.subsonicclient.ui.common.CoverArtImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: String,
    loginRepository: LoginRepository,
    onBack: () -> Unit,
    onPlaySong: (songId: String, title: String?, artist: String?, queueIds: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AlbumDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AlbumDetailViewModel(loginRepository, albumId) as T
            }
        }
    )
    val state by viewModel.uiState.collectAsState()
    val creds by loginRepository.credentials.collectAsState(initial = null)

    fun buildCoverArtUrl(coverArtId: String?): String? = creds?.let {
        SubsonicCoverArtUrlBuilder.build(it.serverUrl, it.username, it.password, coverArtId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.album?.name ?: "アルバム") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                state.album != null -> {
                    val album = state.album!!
                    val songs = album.song.orEmpty()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CoverArtImage(
                                url = buildCoverArtUrl(album.coverArt),
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                contentDescription = album.name,
                                placeholder = {
                                    Text(
                                        album.name.orEmpty().take(1),
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                }
                            )
                        }
                        item {
                            Text(
                                album.artist ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(songs) { song ->
                            SongRow(
                                song = song,
                                coverArtUrl = buildCoverArtUrl(song.coverArt),
                                onClick = {
                                    val id = song.id ?: return@SongRow
                                    onPlaySong(id, song.title, song.artist, songs.mapNotNull { it.id })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: SongDto, coverArtUrl: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArtImage(
                url = coverArtUrl,
                modifier = Modifier.size(56.dp),
                contentDescription = song.title,
                placeholder = {
                    Text(
                        song.title.orEmpty().take(1),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = song.title.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                Text(text = song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
