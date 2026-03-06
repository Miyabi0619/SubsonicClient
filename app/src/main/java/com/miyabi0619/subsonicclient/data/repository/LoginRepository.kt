package com.miyabi0619.subsonicclient.data.repository

import com.miyabi0619.subsonicclient.data.api.SubsonicApi
import com.miyabi0619.subsonicclient.data.api.SubsonicClientFactory
import com.miyabi0619.subsonicclient.data.prefs.CredentialsStore
import com.miyabi0619.subsonicclient.data.prefs.SubsonicCredentials
import kotlinx.coroutines.flow.Flow

class LoginRepository(
    private val credentialsStore: CredentialsStore
) {

    val credentials: Flow<SubsonicCredentials?> = credentialsStore.credentials

    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> {
        return runCatching {
            val api = SubsonicClientFactory.create(
                serverUrl = serverUrl,
                username = username,
                password = password
            )
            val envelope = api.ping()
            val body = envelope.response
                ?: throw IllegalStateException("Empty response")
            body.error?.let { err ->
                throw IllegalStateException(err.message ?: "API error ${err.code}")
            }
            if (body.status != "ok") {
                throw IllegalStateException("Unexpected status: ${body.status}")
            }
            credentialsStore.save(serverUrl, username, password)
        }
    }

    suspend fun logout() {
        credentialsStore.clear()
    }

    fun createApi(creds: SubsonicCredentials): SubsonicApi =
        SubsonicClientFactory.create(
            serverUrl = creds.serverUrl,
            username = creds.username,
            password = creds.password
        )
}
