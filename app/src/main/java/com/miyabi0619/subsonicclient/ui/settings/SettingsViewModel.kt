package com.miyabi0619.subsonicclient.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyabi0619.subsonicclient.data.prefs.AppSettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val appSettingsStore: AppSettingsStore) : ViewModel() {

    val maxBitRate: StateFlow<Int> = appSettingsStore.maxBitRate
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setMaxBitRate(value: Int) {
        viewModelScope.launch {
            appSettingsStore.setMaxBitRate(value)
        }
    }
}
