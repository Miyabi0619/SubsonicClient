package com.miyabi0619.subsonicclient.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Subsonic / OpenSubsonic 互換 REST API（Navidrome 等）.
 * 認証パラメータ (u, t, s, v, c, f) は [SubsonicAuthInterceptor] で付与する.
 * ストリーミングURLは [SubsonicStreamUrlBuilder] で生成する.
 */
interface SubsonicApi {

    @GET("ping.view")
    suspend fun ping(): SubsonicEnvelope

    @GET("getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0
    ): SubsonicEnvelope

    @GET("getRandomSongs.view")
    suspend fun getRandomSongs(@Query("size") size: Int = 50): SubsonicEnvelope

    @GET("getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope

    @GET("getArtists.view")
    suspend fun getArtists(): SubsonicEnvelope

    @GET("getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope

    @GET("getGenres.view")
    suspend fun getGenres(): SubsonicEnvelope

    @GET("getSongsByGenre.view")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("count") count: Int = 50,
        @Query("offset") offset: Int = 0
    ): SubsonicEnvelope

    @GET("getPlaylists.view")
    suspend fun getPlaylists(): SubsonicEnvelope

    @GET("getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicEnvelope

    @GET("search3.view")
    suspend fun search3(@Query("query") query: String): SubsonicEnvelope

    @GET("getLyrics.view")
    suspend fun getLyrics(
        @Query("artist") artist: String? = null,
        @Query("title") title: String? = null
    ): SubsonicEnvelope
}
