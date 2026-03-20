package com.miyabi0619.subsonicclient.ui.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.miyabi0619.subsonicclient.data.api.AlbumDto
import com.miyabi0619.subsonicclient.data.api.SubsonicCoverArtUrlBuilder
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import com.miyabi0619.subsonicclient.ui.common.CoverArtImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    loginRepository: LoginRepository,
    onBack: () -> Unit,
    onAlbumClick: (albumId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ArtistDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ArtistDetailViewModel(loginRepository, artistId) as T
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
                title = { Text(state.artist?.name ?: "アーティスト") },
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
                state.artist != null -> {
                    val albums = state.artist!!.album.orEmpty()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(albums) { album ->
                            AlbumRow(
                                album = album,
                                coverArtUrl = buildCoverArtUrl(album.coverArt),
                                onClick = {
                                    val id = album.id ?: return@AlbumRow
                                    onAlbumClick(id)
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
private fun AlbumRow(album: AlbumDto, coverArtUrl: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverArtImage(
                url = coverArtUrl,
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
                album.artist?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
