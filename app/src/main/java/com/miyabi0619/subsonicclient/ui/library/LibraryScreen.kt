package com.miyabi0619.subsonicclient.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.miyabi0619.subsonicclient.data.api.GenreDto
import com.miyabi0619.subsonicclient.data.api.PlaylistDto
import com.miyabi0619.subsonicclient.data.api.SubsonicCoverArtUrlBuilder
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import com.miyabi0619.subsonicclient.ui.common.CoverArtImage

@Composable
fun LibraryScreen(
    loginRepository: LoginRepository,
    onAlbumClick: (albumId: String) -> Unit = {},
    onArtistClick: (artistId: String) -> Unit = {},
    onPlaylistClick: (playlistId: String) -> Unit = {},
    onGenreClick: (genreName: String) -> Unit = {},
    viewModel: LibraryViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LibraryViewModel(loginRepository) as T
            }
        }
    ),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val creds by loginRepository.credentials.collectAsState(initial = null)

    fun buildCoverArtUrl(coverArtId: String?): String? = creds?.let {
        SubsonicCoverArtUrlBuilder.build(it.serverUrl, it.username, it.password, coverArtId)
    }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                LibraryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    LibraryTab.Albums -> "アルバム"
                                    LibraryTab.Artists -> "アーティスト"
                                    LibraryTab.Genres -> "ジャンル"
                                    LibraryTab.Playlists -> "プレイリスト"
                                }
                            )
                        }
                    )
                }
            }
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                state.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }
                else -> when (state.selectedTab) {
                    LibraryTab.Albums -> LibraryAlbumList(
                        albums = state.albums,
                        buildCoverArtUrl = ::buildCoverArtUrl,
                        onAlbumClick = onAlbumClick
                    )
                    LibraryTab.Artists -> LibraryArtistList(
                        artists = state.artists,
                        buildCoverArtUrl = ::buildCoverArtUrl,
                        onArtistClick = onArtistClick
                    )
                    LibraryTab.Genres -> LibraryGenreList(
                        genres = state.genres,
                        onGenreClick = onGenreClick
                    )
                    LibraryTab.Playlists -> LibraryPlaylistList(
                        playlists = state.playlists,
                        onPlaylistClick = onPlaylistClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryAlbumList(
    albums: List<AlbumDto>,
    buildCoverArtUrl: (String?) -> String?,
    onAlbumClick: (albumId: String) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val id = album.id ?: return@clickable
                        onAlbumClick(id)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArtImage(
                        url = buildCoverArtUrl(album.coverArt),
                        modifier = Modifier.size(64.dp),
                        contentDescription = album.title,
                        placeholder = {
                            Text(
                                album.title.orEmpty().ifEmpty { album.name.orEmpty() }.take(1),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = album.title.orEmpty().ifEmpty { album.name.orEmpty() },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        album.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryArtistList(
    artists: List<ArtistDto>,
    buildCoverArtUrl: (String?) -> String?,
    onArtistClick: (artistId: String) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artists) { artist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val id = artist.id ?: return@clickable
                        onArtistClick(id)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArtImage(
                        url = artist.artistImageUrl ?: buildCoverArtUrl(artist.coverArt),
                        modifier = Modifier.size(64.dp),
                        contentDescription = artist.name,
                        placeholder = {
                            Text(
                                artist.name.orEmpty().take(1),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    )
                    Text(
                        text = artist.name.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryGenreList(
    genres: List<GenreDto>,
    onGenreClick: (genreName: String) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    val name = genre.value ?: return@clickable
                    onGenreClick(name)
                },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = genre.value.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "曲: ${genre.songCount ?: 0} / アルバム: ${genre.albumCount ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryPlaylistList(
    playlists: List<PlaylistDto>,
    onPlaylistClick: (playlistId: String) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(playlists) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val id = playlist.id ?: return@clickable
                        onPlaylistClick(id)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = playlist.name.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "曲数: ${playlist.songCount ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}
