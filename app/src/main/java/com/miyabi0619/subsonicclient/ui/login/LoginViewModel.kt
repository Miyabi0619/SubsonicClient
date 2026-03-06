package com.miyabi0619.subsonicclient.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.prefs.SubsonicCredentials
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel(
    private val loginRepository: LoginRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loginRepository.credentials
                .catch { _ -> emit(null) }
                .collect { creds ->
                    _uiState.update { it.copy(isLoggedIn = creds != null) }
                }
        }
    }

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, error = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        val url = _uiState.value.serverUrl.trim()
        val user = _uiState.value.username.trim()
        val pass = _uiState.value.password
        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(error = "URL・ユーザー名・パスワードを入力してください") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            loginRepository.login(url, user, pass)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, error = null) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "ログインに失敗しました"
                        )
                    }
                }
        }
    }

    private fun MutableStateFlow<LoginUiState>.update(block: (LoginUiState) -> LoginUiState) {
        value = block(value)
    }
}
