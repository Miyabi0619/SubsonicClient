package com.miyabi0619.subsonicclient.ui.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.api.ArtistDetail
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val artist: ArtistDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ArtistDetailViewModel(
    private val loginRepository: LoginRepository,
    private val artistId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

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
                val envelope = api.getArtist(artistId)
                val artist = envelope.response?.artist
                _uiState.value = _uiState.value.copy(
                    artist = artist,
                    isLoading = false,
                    error = if (artist == null) "アーティストを取得できませんでした" else null
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
