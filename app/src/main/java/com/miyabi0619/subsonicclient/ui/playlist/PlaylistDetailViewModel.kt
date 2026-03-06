package com.miyabi0619.subsonicclient.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.PlaylistDetail
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val playlist: PlaylistDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PlaylistDetailViewModel(
    private val loginRepository: LoginRepository,
    private val playlistId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

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
                val envelope = api.getPlaylist(playlistId)
                val playlist = envelope.response?.playlist
                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    isLoading = false,
                    error = if (playlist == null) "プレイリストを取得できませんでした" else null
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
