package com.miyabi0619.subsonicclient.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SubsonicClientFactory {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * baseUrl は "https://example.com" または "https://example.com/subsonic" 形式.
     * 末尾に /rest/ がなければ付与する.
     */
    fun normalizeBaseUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim().trimEnd('/')
        return if (trimmed.endsWith("/rest")) trimmed
        else if (trimmed.endsWith("/rest/")) trimmed
        else "$trimmed/rest"
    }

    fun create(
        serverUrl: String,
        username: String,
        password: String,
        enableLogging: Boolean = false
    ): SubsonicApi {
        val baseUrl = normalizeBaseUrl(serverUrl).let {
            if (!it.endsWith("/")) "$it/" else it
        }
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(SubsonicAuthInterceptor(username, password))
        if (enableLogging) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            )
        }
        val client = clientBuilder.build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SubsonicApi::class.java)
    }
}
