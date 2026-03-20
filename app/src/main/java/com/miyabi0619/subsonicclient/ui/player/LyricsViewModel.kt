package com.miyabi0619.subsonicclient.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LyricsUiState(
    val lyrics: String? = null,
    val isLoading: Boolean = false,
    val isUnavailable: Boolean = false
)

class LyricsViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    fun loadLyrics(artist: String?, title: String?) {
        if (artist.isNullOrBlank() && title.isNullOrBlank()) {
            _uiState.value = LyricsUiState(isUnavailable = true)
            return
        }
        viewModelScope.launch {
            val creds = loginRepository.credentials.first() ?: return@launch
            _uiState.value = LyricsUiState(isLoading = true)
            val api = loginRepository.createApi(creds)
            runCatching {
                val envelope = api.getLyrics(artist = artist, title = title)
                val text = envelope.response?.lyrics?.value
                _uiState.value = if (text.isNullOrBlank()) {
                    LyricsUiState(isUnavailable = true)
                } else {
                    LyricsUiState(lyrics = text)
                }
            }.onFailure {
                _uiState.value = LyricsUiState(isUnavailable = true)
            }
        }
    }
}
