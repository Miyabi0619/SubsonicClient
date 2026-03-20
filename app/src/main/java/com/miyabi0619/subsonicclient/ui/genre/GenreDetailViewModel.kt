package com.miyabi0619.subsonicclient.ui.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GenreDetailUiState(
    val genreName: String = "",
    val songs: List<SongDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GenreDetailViewModel(
    private val loginRepository: LoginRepository,
    private val genreName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenreDetailUiState(genreName = genreName))
    val uiState: StateFlow<GenreDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val creds = loginRepository.credentials.first() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val api = loginRepository.createApi(creds)
            runCatching {
                val envelope = api.getSongsByGenre(genreName)
                val songs = envelope.response?.songsByGenre?.song.orEmpty()
                _uiState.value = _uiState.value.copy(songs = songs, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "読み込みに失敗しました"
                )
            }
        }
    }
}
