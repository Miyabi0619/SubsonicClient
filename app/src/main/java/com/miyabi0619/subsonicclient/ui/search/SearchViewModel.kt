package com.miyabi0619.subsonicclient.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.AlbumDto
import com.miyabi0619.subsonicclient.data.api.ArtistDto
import com.miyabi0619.subsonicclient.data.api.PlaylistDto
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val songs: List<SongDto> = emptyList(),
    val albums: List<AlbumDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query, error = null)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                songs = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                playlists = emptyList()
            )
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch(query)
        }
    }

    fun search() {
        val q = _uiState.value.query.trim()
        if (q.isBlank()) return
        viewModelScope.launch { performSearch(q) }
    }

    private suspend fun performSearch(query: String) {
        val creds = loginRepository.credentials.first() ?: run {
            _uiState.value = _uiState.value.copy(error = "未ログイン")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        val api = loginRepository.createApi(creds)
        runCatching {
            val envelope = api.search3(query = query.trim())
            val result = envelope.response?.searchResult3
            _uiState.value = _uiState.value.copy(
                songs = result?.song.orEmpty(),
                albums = result?.album.orEmpty(),
                artists = result?.artist.orEmpty(),
                playlists = result?.playlist.orEmpty(),
                isLoading = false,
                error = null
            )
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "検索に失敗しました"
            )
        }
    }
}
