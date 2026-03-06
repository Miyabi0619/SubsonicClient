package com.miyabi0619.subsonicclient.data.api

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest

/**
 * Subsonic トークン認証: token = md5(password + salt), 各リクエストで salt をランダム生成.
 */
object SubsonicAuth {

    private const val SALT_LENGTH = 12
    private const val SALT_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun token(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val input = (password + salt).toByteArray(Charsets.UTF_8)
        val hash = digest.digest(input)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): String =
        (1..SALT_LENGTH).map { SALT_CHARS.random() }.joinToString("")

    fun authParams(username: String, password: String): Map<String, String> {
        val salt = generateSalt()
        val t = token(password, salt)
        return mapOf(
            "u" to username,
            "t" to t,
            "s" to salt,
            "v" to SUBSONIC_API_VERSION,
            "c" to SUBSONIC_CLIENT_NAME,
            "f" to "json"
        )
    }
}

/**
 * 全リクエストに Subsonic 認証クエリパラメータを付与する OkHttp Interceptor.
 */
class SubsonicAuthInterceptor(
    private val username: String,
    private val password: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url
        val params = SubsonicAuth.authParams(username, password)
        val newUrl = url.newBuilder().apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()
        val newRequest = original.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }
}

/**
 * ストリーミング再生用 URL を組み立てる（ExoPlayer に渡す）.
 */
object SubsonicStreamUrlBuilder {

    fun build(
        baseUrl: String,
        username: String,
        password: String,
        songId: String,
        maxBitRate: Int? = null,
        format: String? = null
    ): String {
        val restBase = baseUrl.trimEnd('/') + "/rest/"
        val params = SubsonicAuth.authParams(username, password).toMutableMap()
        params["id"] = songId
        maxBitRate?.let { params["maxBitRate"] = it.toString() }
        format?.let { params["format"] = it }
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }
        return "${restBase}stream.view?$query"
    }
}
