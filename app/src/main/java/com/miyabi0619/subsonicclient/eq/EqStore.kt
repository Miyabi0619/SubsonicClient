package com.miyabi0619.subsonicclient.eq

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.StateFlow
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.eqDataStore: DataStore<Preferences> by preferencesDataStore(name = "eq")

class EqStore(private val context: Context) {

    private val keyEnabled = booleanPreferencesKey("enabled")
    private val keyGains = (0 until 10).map { i -> floatPreferencesKey("gain_$i") }
    private val keyHardwareAvailable = booleanPreferencesKey("hardware_available")

    val eqState: Flow<EqState> = context.eqDataStore.data.map { prefs ->
        val enabled = prefs[keyEnabled] ?: true
        val gains = keyGains.map { prefs[it] ?: 0f }
        val hw = prefs[keyHardwareAvailable]
        createEqStateFromGains(gains).copy(enabled = enabled, hardwareAvailable = hw)
    }

    suspend fun setHardwareAvailable(available: Boolean) {
        context.eqDataStore.edit { it[keyHardwareAvailable] = available }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.eqDataStore.edit { it[keyEnabled] = enabled }
    }

    suspend fun setGains(gains: List<Float>) {
        context.eqDataStore.edit { prefs ->
            gains.forEachIndexed { i, g ->
                if (i < keyGains.size) prefs[keyGains[i]] = g.coerceIn(-12f, 12f)
            }
        }
    }

    suspend fun setEqState(state: EqState) {
        context.eqDataStore.edit { prefs ->
            prefs[keyEnabled] = state.enabled
            state.bands.forEachIndexed { i, band ->
                if (i < keyGains.size) prefs[keyGains[i]] = band.gainDb.coerceIn(-12f, 12f)
            }
        }
    }
}
