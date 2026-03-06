package com.miyabi0619.subsonicclient.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.AlbumDto
import com.miyabi0619.subsonicclient.data.api.ArtistDto
import com.miyabi0619.subsonicclient.data.api.GenreDto
import com.miyabi0619.subsonicclient.data.api.PlaylistDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LibraryTab {
    Albums,
    Artists,
    Genres,
    Playlists
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.Albums,
    val albums: List<AlbumDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val playlists: List<PlaylistDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LibraryViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadIfNeeded(tab)
    }

    fun load() {
        loadIfNeeded(_uiState.value.selectedTab)
    }

    private fun loadIfNeeded(tab: LibraryTab) {
        viewModelScope.launch {
            val creds = loginRepository.credentials.first() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val api = loginRepository.createApi(creds)
            runCatching {
                when (tab) {
                    LibraryTab.Albums -> {
                        val envelope = api.getAlbumList2(type = "alphabeticalByName", size = 200)
                        val list = envelope.response?.albumList2?.album.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            albums = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    LibraryTab.Artists -> {
                        val envelope = api.getArtists()
                        val indexList = envelope.response?.artists?.index.orEmpty()
                        val list = indexList.flatMap { it.artist.orEmpty() }
                        _uiState.value = _uiState.value.copy(
                            artists = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    LibraryTab.Genres -> {
                        val envelope = api.getGenres()
                        val list = envelope.response?.genres?.genre.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            genres = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    LibraryTab.Playlists -> {
                        val envelope = api.getPlaylists()
                        val list = envelope.response?.playlists?.playlist.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            playlists = list,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "読み込みに失敗しました"
                )
            }
        }
    }

    init {
        load()
    }
}
