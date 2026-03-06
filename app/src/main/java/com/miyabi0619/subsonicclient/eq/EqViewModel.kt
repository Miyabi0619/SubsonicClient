package com.miyabi0619.subsonicclient.eq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EqViewModel(
    private val eqStore: EqStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqState(bands = emptyList()))
    val uiState: StateFlow<EqState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            eqStore.eqState.collect { _uiState.value = it }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            eqStore.setEnabled(enabled)
        }
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        viewModelScope.launch {
            val state = _uiState.value
            val newBands = state.bands.mapIndexed { i, b ->
                if (i == bandIndex) b.copy(gainDb = gainDb.coerceIn(-12f, 12f)) else b
            }
            eqStore.setGains(newBands.map { it.gainDb })
        }
    }

    fun applyPreset(preset: EqPresetType) {
        viewModelScope.launch {
            eqStore.setEqState(preset.toEqState())
        }
    }

    fun resetToFlat() {
        viewModelScope.launch {
            eqStore.setEqState(createFlatEqState())
        }
    }
}
