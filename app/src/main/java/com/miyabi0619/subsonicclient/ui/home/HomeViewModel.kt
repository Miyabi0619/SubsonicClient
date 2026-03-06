package com.miyabi0619.subsonicclient.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.AlbumDto
import com.miyabi0619.subsonicclient.data.api.ArtistDto
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val recentAlbums: List<AlbumDto> = emptyList(),
    val randomSongs: List<SongDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val creds = loginRepository.credentials.first() ?: run {
                _uiState.value = _uiState.value.copy(error = "未ログイン")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val api = loginRepository.createApi(creds)
            runCatching {
                val albumEnvelope = api.getAlbumList2(type = "recent", size = 20)
                val songEnvelope = api.getRandomSongs(size = 15)
                val artistsEnvelope = api.getArtists()
                val recentAlbums = albumEnvelope.response?.albumList2?.album.orEmpty()
                val randomSongs = songEnvelope.response?.randomSongs?.song.orEmpty()
                val indexList = artistsEnvelope.response?.artists?.index.orEmpty()
                val artists = indexList.flatMap { it.artist.orEmpty() }
                _uiState.value = _uiState.value.copy(
                    recentAlbums = recentAlbums,
                    randomSongs = randomSongs,
                    artists = artists,
                    isLoading = false,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "読み込みに失敗しました"
                )
            }
        }
    }
}
