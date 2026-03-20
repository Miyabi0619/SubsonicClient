package com.miyabi0619.subsonicclient.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

const val SUBSONIC_API_VERSION = "1.16.0"
const val SUBSONIC_CLIENT_NAME = "SubsonicClient"

/** 全エンドポイント共通: subsonic-response でラップされた envelope */
@JsonClass(generateAdapter = true)
data class SubsonicEnvelope(
    @Json(name = "subsonic-response") val response: SubsonicBody?
) {
    val body: SubsonicBody get() = response!!
}

@JsonClass(generateAdapter = true)
data class SubsonicBody(
    @Json(name = "status") val status: String?,
    @Json(name = "version") val version: String?,
    @Json(name = "error") val error: SubsonicError?,
    @Json(name = "ping") val ping: SubsonicPing?,
    @Json(name = "albumList2") val albumList2: AlbumList2?,
    @Json(name = "randomSongs") val randomSongs: RandomSongs?,
    @Json(name = "album") val album: AlbumDetail?,
    @Json(name = "artists") val artists: Artists?,
    @Json(name = "artist") val artist: ArtistDetail?,
    @Json(name = "genres") val genres: Genres?,
    @Json(name = "songsByGenre") val songsByGenre: SongsByGenre?,
    @Json(name = "playlists") val playlists: Playlists?,
    @Json(name = "playlist") val playlist: PlaylistDetail?,
    @Json(name = "searchResult3") val searchResult3: SearchResult3?,
    @Json(name = "lyrics") val lyrics: LyricsDto?
)

@JsonClass(generateAdapter = true)
data class SubsonicError(
    @Json(name = "code") val code: Int?,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class SubsonicPing(
    @Json(name = "status") val status: String? = "ok"
)

@JsonClass(generateAdapter = true)
data class AlbumList2(
    @Json(name = "album") val album: List<AlbumDto>?
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    @Json(name = "id") val id: String?,
    @Json(name = "parent") val parent: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "artist") val artist: String?,
    @Json(name = "artistId") val artistId: String?,
    @Json(name = "coverArt") val coverArt: String?,
    @Json(name = "songCount") val songCount: Int?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "playCount") val playCount: Int?,
    @Json(name = "year") val year: Int?,
    @Json(name = "created") val created: String?
)

@JsonClass(generateAdapter = true)
data class RandomSongs(
    @Json(name = "song") val song: List<SongDto>?
)

@JsonClass(generateAdapter = true)
data class SongDto(
    @Json(name = "id") val id: String?,
    @Json(name = "parent") val parent: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "album") val album: String?,
    @Json(name = "artist") val artist: String?,
    @Json(name = "albumId") val albumId: String?,
    @Json(name = "artistId") val artistId: String?,
    @Json(name = "coverArt") val coverArt: String?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "playCount") val playCount: Int?,
    @Json(name = "track") val track: Int?,
    @Json(name = "year") val year: Int?,
    @Json(name = "suffix") val suffix: String?,
    @Json(name = "contentType") val contentType: String?,
    @Json(name = "size") val size: Long?,
    @Json(name = "bitRate") val bitRate: Int?,
    @Json(name = "path") val path: String?
)

@JsonClass(generateAdapter = true)
data class AlbumDetail(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "artist") val artist: String?,
    @Json(name = "artistId") val artistId: String?,
    @Json(name = "coverArt") val coverArt: String?,
    @Json(name = "songCount") val songCount: Int?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "year") val year: Int?,
    @Json(name = "song") val song: List<SongDto>?
)

@JsonClass(generateAdapter = true)
data class Artists(
    @Json(name = "index") val index: List<ArtistIndexDto>?
)

@JsonClass(generateAdapter = true)
data class ArtistIndexDto(
    @Json(name = "name") val name: String?,
    @Json(name = "artist") val artist: List<ArtistDto>?
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "coverArt") val coverArt: String?,
    @Json(name = "albumCount") val albumCount: Int?,
    @Json(name = "artistImageUrl") val artistImageUrl: String?
)

@JsonClass(generateAdapter = true)
data class ArtistDetail(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "coverArt") val coverArt: String?,
    @Json(name = "albumCount") val albumCount: Int?,
    @Json(name = "album") val album: List<AlbumDto>?
)

@JsonClass(generateAdapter = true)
data class Genres(
    @Json(name = "genre") val genre: List<GenreDto>?
)

@JsonClass(generateAdapter = true)
data class GenreDto(
    @Json(name = "value") val value: String?,
    @Json(name = "songCount") val songCount: Int?,
    @Json(name = "albumCount") val albumCount: Int?
)

@JsonClass(generateAdapter = true)
data class SongsByGenre(
    @Json(name = "song") val song: List<SongDto>?
)

@JsonClass(generateAdapter = true)
data class Playlists(
    @Json(name = "playlist") val playlist: List<PlaylistDto>?
)

@JsonClass(generateAdapter = true)
data class PlaylistDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "songCount") val songCount: Int?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "owner") val owner: String?,
    @Json(name = "created") val created: String?,
    @Json(name = "coverArt") val coverArt: String?
)

@JsonClass(generateAdapter = true)
data class PlaylistDetail(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "songCount") val songCount: Int?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "owner") val owner: String?,
    @Json(name = "entry") val entry: List<SongDto>?
)

@JsonClass(generateAdapter = true)
data class LyricsDto(
    @Json(name = "artist") val artist: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "value") val value: String?
)

@JsonClass(generateAdapter = true)
data class SearchResult3(
    @Json(name = "song") val song: List<SongDto>? = null,
    @Json(name = "album") val album: List<AlbumDto>? = null,
    @Json(name = "artist") val artist: List<ArtistDto>? = null,
    @Json(name = "playlist") val playlist: List<PlaylistDto>? = null
)
