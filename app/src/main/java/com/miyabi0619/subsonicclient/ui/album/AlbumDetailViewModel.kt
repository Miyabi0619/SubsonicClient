package com.miyabi0619.subsonicclient.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.AlbumDetail
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
    val album: AlbumDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class AlbumDetailViewModel(
    private val loginRepository: LoginRepository,
    private val albumId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

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
                val envelope = api.getAlbum(albumId)
                val album = envelope.response?.album
                _uiState.value = _uiState.value.copy(
                    album = album,
                    isLoading = false,
                    error = if (album == null) "アルバムを取得できませんでした" else null
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
