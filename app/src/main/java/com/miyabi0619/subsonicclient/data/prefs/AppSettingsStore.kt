package com.miyabi0619.subsonicclient.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsStore(private val context: Context) {

    private val keyMaxBitRate = intPreferencesKey("max_bit_rate")

    /** 0 = 制限なし（オリジナル品質）、それ以外は kbps 単位の上限値 */
    val maxBitRate: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[keyMaxBitRate] ?: 0
    }

    suspend fun setMaxBitRate(value: Int) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[keyMaxBitRate] = value
        }
    }
}
