package com.miyabi0619.subsonicclient.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "credentials")

class CredentialsStore(private val context: Context) {

    private val keyServerUrl = stringPreferencesKey("server_url")
    private val keyUsername = stringPreferencesKey("username")
    private val keyPassword = stringPreferencesKey("password")

    val credentials: Flow<SubsonicCredentials?> = context.dataStore.data.map { prefs ->
        val url = prefs[keyServerUrl] ?: return@map null
        val user = prefs[keyUsername] ?: return@map null
        val pass = prefs[keyPassword] ?: return@map null
        SubsonicCredentials(serverUrl = url, username = user, password = pass)
    }

    suspend fun save(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[keyServerUrl] = serverUrl.trim()
            prefs[keyUsername] = username
            prefs[keyPassword] = password
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

data class SubsonicCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)
