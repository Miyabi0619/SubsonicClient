package com.miyabi0619.subsonicclient.ui.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository

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
        when {
            state.isLoading -> Alignment.CenterHorizontally.let { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            state.album != null -> {
                val album = state.album!!
                val songs = album.song.orEmpty()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            album.artist ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(songs) { song ->
                        SongRow(
                            song = song,
                            onClick = {
                                val id = song.id ?: return@SongRow
                                onPlaySong(
                                    id,
                                    song.title,
                                    song.artist,
                                    songs.mapNotNull { it.id }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: SongDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = song.artist.orEmpty(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
