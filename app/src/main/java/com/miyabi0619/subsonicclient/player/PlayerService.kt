package com.miyabi0619.subsonicclient.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.miyabi0619.subsonicclient.MainActivity
import com.miyabi0619.subsonicclient.R
import com.miyabi0619.subsonicclient.data.api.PlaylistDto
import com.miyabi0619.subsonicclient.data.api.SongDto
import com.miyabi0619.subsonicclient.data.api.SubsonicApi
import com.miyabi0619.subsonicclient.data.api.SubsonicClientFactory
import com.miyabi0619.subsonicclient.data.api.SubsonicCoverArtUrlBuilder
import com.miyabi0619.subsonicclient.data.api.SubsonicEnvelope
import com.miyabi0619.subsonicclient.data.api.SubsonicStreamUrlBuilder
import com.miyabi0619.subsonicclient.data.prefs.AppSettingsStore
import com.miyabi0619.subsonicclient.data.prefs.CredentialsStore
import com.miyabi0619.subsonicclient.data.prefs.SubsonicCredentials
import com.miyabi0619.subsonicclient.eq.EqApplier
import com.miyabi0619.subsonicclient.eq.EqStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class PlayerService : MediaLibraryService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private var mediaSession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private lateinit var credentialsStore: CredentialsStore
    private lateinit var eqStore: EqStore
    private lateinit var appSettingsStore: AppSettingsStore
    private lateinit var songMetadataCachePreferences: SharedPreferences
    private var eqApplier: EqApplier? = null
    private var eqStateJob: Job? = null
    private val songCache = ConcurrentHashMap<String, CachedSong>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        credentialsStore = CredentialsStore(applicationContext)
        eqStore = EqStore(applicationContext)
        appSettingsStore = AppSettingsStore(applicationContext)
        songMetadataCachePreferences = getSharedPreferences(SONG_METADATA_CACHE_PREFS, Context.MODE_PRIVATE)
        val exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        5_000,
                        30_000,
                        750,
                        1_000
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        attachEqIfNeeded(audioSessionId)
                    }
                })
            }
        player = exoPlayer
        mediaSession = MediaLibrarySession.Builder(this, exoPlayer, LibraryCallback())
            .setSessionActivity(createSessionActivityPendingIntent())
            .build()
    }

    private fun createSessionActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val songId = intent.getStringExtra(EXTRA_SONG_ID)
                val queueIds = intent.getStringArrayListExtra(EXTRA_QUEUE_IDS)
                val title = intent.getStringExtra(EXTRA_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                if (!songId.isNullOrBlank()) {
                    startForeground(NOTIFICATION_ID, createPlaceholderNotification())
                    serviceScope.launch {
                        playSong(songId, queueIds ?: arrayListOf(songId), title, artist)
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            runLibraryTask(params, page, pageSize) { context ->
                when {
                    parentId == MEDIA_ID_ROOT -> listOf(
                        folderItem(MEDIA_ID_RANDOM, "ランダム再生"),
                        folderItem(MEDIA_ID_PLAYLISTS, "プレイリスト")
                    )

                    parentId == MEDIA_ID_RANDOM ->
                        context.api.getRandomSongs(RANDOM_SONG_COUNT)
                            .bodyOrThrow()
                            .randomSongs
                            ?.song
                            .orEmpty()
                            .map { it.toPlayableMediaItem(context) }

                    parentId == MEDIA_ID_PLAYLISTS ->
                        context.api.getPlaylists()
                            .bodyOrThrow()
                            .playlists
                            ?.playlist
                            .orEmpty()
                            .map { it.toBrowsableMediaItem(context.credentials) }

                    parentId.startsWith(MEDIA_ID_PLAYLIST_PREFIX) -> {
                        val playlistId = parentId.removePrefix(MEDIA_ID_PLAYLIST_PREFIX)
                        context.api.getPlaylist(playlistId)
                            .bodyOrThrow()
                            .playlist
                            ?.entry
                            .orEmpty()
                            .map { it.toPlayableMediaItem(context) }
                    }

                    else -> emptyList()
                }
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            if (mediaId == MEDIA_ID_ROOT) {
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem(), null))
            }
            if (mediaId == MEDIA_ID_RANDOM) {
                return Futures.immediateFuture(LibraryResult.ofItem(folderItem(MEDIA_ID_RANDOM, "ランダム再生"), null))
            }
            if (mediaId == MEDIA_ID_PLAYLISTS) {
                return Futures.immediateFuture(LibraryResult.ofItem(folderItem(MEDIA_ID_PLAYLISTS, "プレイリスト"), null))
            }
            if (mediaId.startsWith(MEDIA_ID_PLAYLIST_PREFIX)) {
                return Futures.immediateFuture(LibraryResult.ofItem(folderItem(mediaId, "プレイリスト"), null))
            }

            val future = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch(Dispatchers.IO) {
                val item = runCatching {
                    val context = autoContext() ?: return@runCatching loginRequiredItem()
                    playableItemForSongId(mediaId, null, context)
                }.getOrElse {
                    unavailableItem("読み込みに失敗しました", it.userFacingMessage())
                }
                future.set(LibraryResult.ofItem(item, null))
            }
            return future
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            serviceScope.launch(Dispatchers.IO) {
                val resolved = runCatching {
                    val context = autoContext() ?: return@runCatching mediaItems.map { loginRequiredItem() }
                    mediaItems.map { item -> playableItemForPlayback(item, context) }
                }.getOrElse { error ->
                    mediaItems.map { unavailableItem("再生できません", error.userFacingMessage()) }
                }
                future.set(resolved)
            }
            return future
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> =
            Futures.immediateFuture(LibraryResult.ofVoid(params))

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            runLibraryTask(params, page, pageSize) { context ->
                val trimmed = query.trim()
                if (trimmed.isBlank()) {
                    emptyList()
                } else {
                    context.api.search3(trimmed)
                        .bodyOrThrow()
                        .searchResult3
                        ?.song
                        .orEmpty()
                        .map { it.toPlayableMediaItem(context) }
                }
            }
    }

    private fun runLibraryTask(
        params: LibraryParams?,
        page: Int,
        pageSize: Int,
        task: suspend (AutoContext) -> List<MediaItem>
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
        serviceScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val context = autoContext() ?: return@runCatching listOf(loginRequiredItem())
                task(context)
            }.getOrElse {
                listOf(unavailableItem("読み込みに失敗しました", it.userFacingMessage()))
            }
            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(paged(result, page, pageSize)), params))
        }
        return future
    }

    private fun createPlaceholderNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("再生中")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

    private suspend fun autoContext(): AutoContext? {
        val credentials = credentialsStore.credentials.first() ?: return null
        val maxBitRate = appSettingsStore.maxBitRate.first().let { if (it > 0) it else null }
        return AutoContext(
            credentials = credentials,
            api = SubsonicClientFactory.create(
                serverUrl = credentials.serverUrl,
                username = credentials.username,
                password = credentials.password
            ),
            maxBitRate = maxBitRate
        )
    }

    private suspend fun playSong(
        songId: String,
        queueIds: List<String>,
        title: String? = null,
        artist: String? = null
    ) {
        val context = autoContext() ?: return
        val startIndex = queueIds.indexOf(songId).coerceAtLeast(0)
        val mediaItems = queueIds.mapIndexed { index, id ->
            val builder = MediaItem.Builder()
                .setMediaId(id)
                .setUri(streamUrl(id, context))
            if (index == startIndex && (title != null || artist != null)) {
                builder.setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title ?: "")
                        .setArtist(artist ?: "")
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
            }
            builder.build()
        }
        player?.let { p ->
            p.setMediaItems(mediaItems, startIndex, 0L)
            p.prepare()
            p.play()
            attachEqIfNeeded(p.audioSessionId)
        }
    }

    private suspend fun playableItemForSongId(
        mediaId: String,
        metadata: MediaMetadata?,
        context: AutoContext
    ): MediaItem {
        if (mediaId.isBlank() || mediaId.startsWith(MEDIA_ID_UNAVAILABLE_PREFIX)) {
            return unavailableItem("再生できません")
        }
        if (needsMetadataResolve(mediaId, metadata)) {
            cachedSongForId(mediaId)?.let { return it.toPlayableMediaItem(context) }
            runCatching {
                context.api.getSong(mediaId).bodyOrThrow().song
            }.getOrNull()?.let { song ->
                if (!song.id.isNullOrBlank()) {
                    cacheSong(song)
                    return song.toPlayableMediaItem(context)
                }
            }
        }
        return mediaItemFor(mediaId, streamUrl(mediaId, context), metadata)
    }

    private suspend fun playableItemForPlayback(item: MediaItem, context: AutoContext): MediaItem {
        val mediaId = item.mediaId
        if (mediaId.isBlank() || mediaId.startsWith(MEDIA_ID_UNAVAILABLE_PREFIX)) {
            return unavailableItem("再生できません")
        }
        cachedSongForId(mediaId)?.let { return it.toPlayableMediaItem(context) }
        return playableItemForSongId(mediaId, item.mediaMetadata, context)
    }

    private fun mediaItemFor(mediaId: String, uri: String, metadata: MediaMetadata?): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(metadata?.title ?: mediaId)
            .setArtist(metadata?.artist ?: "")
            .setAlbumTitle(metadata?.albumTitle)
            .setArtworkUri(metadata?.artworkUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun needsMetadataResolve(mediaId: String, metadata: MediaMetadata?): Boolean {
        val title = metadata?.title?.toString()
        return title.isNullOrBlank() || title == mediaId
    }

    private fun SongDto.toPlayableMediaItem(context: AutoContext): MediaItem {
        val songId = id
        if (songId.isNullOrBlank()) {
            return unavailableItem(title ?: "再生できない曲")
        }
        cacheSong(this)
        val metadata = MediaMetadata.Builder()
            .setTitle(title ?: "Untitled")
            .setArtist(artist?.takeIf { it.isNotBlank() } ?: "[Unknown Artist]")
            .setAlbumTitle(album)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        coverArtUri(coverArt, context.credentials)?.let { metadata.setArtworkUri(it) }
        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(streamUrl(songId, context))
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun CachedSong.toPlayableMediaItem(context: AutoContext): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title ?: "Untitled")
            .setArtist(artist?.takeIf { it.isNotBlank() } ?: "[Unknown Artist]")
            .setAlbumTitle(album)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        coverArtUri(coverArt, context.credentials)?.let { metadata.setArtworkUri(it) }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl(id, context))
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun PlaylistDto.toBrowsableMediaItem(credentials: SubsonicCredentials): MediaItem {
        val playlistId = id
        if (playlistId.isNullOrBlank()) {
            return unavailableItem(name ?: "プレイリスト")
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(name ?: "プレイリスト")
            .setSubtitle(songCount?.let { "${it}曲" })
            .setIsBrowsable(true)
            .setIsPlayable(false)
        coverArtUri(coverArt, credentials)?.let { metadata.setArtworkUri(it) }
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID_PLAYLIST_PREFIX + playlistId)
            .setMediaMetadata(metadata.build())
            .build()
    }

    private fun rootItem(): MediaItem = folderItem(MEDIA_ID_ROOT, "Subsonic")

    private fun folderItem(mediaId: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun loginRequiredItem(): MediaItem = unavailableItem("ログインが必要です")

    private fun unavailableItem(title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(MEDIA_ID_UNAVAILABLE_PREFIX + title)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun streamUrl(songId: String, context: AutoContext): String =
        SubsonicStreamUrlBuilder.build(
            baseUrl = context.credentials.serverUrl,
            username = context.credentials.username,
            password = context.credentials.password,
            songId = songId,
            maxBitRate = context.maxBitRate
        )

    private fun coverArtUri(coverArtId: String?, credentials: SubsonicCredentials): Uri? =
        SubsonicCoverArtUrlBuilder.build(
            serverUrl = credentials.serverUrl,
            username = credentials.username,
            password = credentials.password,
            coverArtId = coverArtId,
            size = 300
        )?.let(Uri::parse)

    private fun SubsonicEnvelope.bodyOrThrow() =
        response?.also { body ->
            body.error?.let { error ->
                throw IOException(error.message ?: "Subsonic error ${error.code ?: ""}".trim())
            }
        } ?: throw IOException("Subsonic response is empty")

    private fun Throwable.userFacingMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    private fun cacheSong(song: SongDto) {
        val songId = song.id?.takeIf { it.isNotBlank() } ?: return
        val cachedSong = CachedSong(
            id = songId,
            title = song.title,
            artist = song.artist,
            album = song.album,
            coverArt = song.coverArt
        )
        songCache[songId] = cachedSong
        val json = JSONObject()
            .put("id", cachedSong.id)
            .put("title", cachedSong.title)
            .put("artist", cachedSong.artist)
            .put("album", cachedSong.album)
            .put("coverArt", cachedSong.coverArt)
        songMetadataCachePreferences.edit().putString(songId, json.toString()).apply()
    }

    private fun cachedSongForId(mediaId: String): CachedSong? {
        songCache[mediaId]?.let { return it }
        val rawJson = songMetadataCachePreferences.getString(mediaId, null) ?: return null
        return runCatching {
            val json = JSONObject(rawJson)
            CachedSong(
                id = json.nullableString("id") ?: return null,
                title = json.nullableString("title"),
                artist = json.nullableString("artist"),
                album = json.nullableString("album"),
                coverArt = json.nullableString("coverArt")
            )
        }.getOrNull()?.also { song ->
            if (song.id.isNotBlank()) {
                songCache[song.id] = song
            }
        }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key) else null

    private fun paged(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
        if (pageSize == Int.MAX_VALUE) return items
        val safePageSize = pageSize.coerceAtLeast(0)
        val fromIndex = page.coerceAtLeast(0) * safePageSize
        if (fromIndex >= items.size) return emptyList()
        val toIndex = (fromIndex + safePageSize).coerceAtMost(items.size)
        return items.subList(fromIndex, toIndex)
    }

    private fun attachEqIfNeeded(audioSessionId: Int) {
        if (audioSessionId == 0) return
        eqStateJob?.cancel()
        eqApplier?.release()
        val applier = EqApplier(audioSessionId)
        val attached = applier.attach()
        serviceScope.launch { eqStore.setHardwareAvailable(attached) }
        if (attached) {
            serviceScope.launch {
                eqStore.eqState.first().let { applier.apply(it) }
            }
            eqStateJob = eqStore.eqState.onEach { applier.apply(it) }.launchIn(serviceScope)
            eqApplier = applier
        }
    }

    override fun onDestroy() {
        eqStateJob?.cancel()
        eqStateJob = null
        eqApplier?.release()
        eqApplier = null
        mediaSession?.run {
            player?.release()
            release()
        }
        mediaSession = null
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private data class AutoContext(
        val credentials: SubsonicCredentials,
        val api: SubsonicApi,
        val maxBitRate: Int?
    )

    private data class CachedSong(
        val id: String,
        val title: String?,
        val artist: String?,
        val album: String?,
        val coverArt: String?
    )

    companion object {
        const val ACTION_PLAY = "com.miyabi0619.subsonicclient.PLAY"
        const val EXTRA_SONG_ID = "song_id"
        const val EXTRA_QUEUE_IDS = "queue_ids"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
        private const val SONG_METADATA_CACHE_PREFS = "song_metadata_cache"
        private const val MEDIA_ID_ROOT = "root"
        private const val MEDIA_ID_RANDOM = "random"
        private const val MEDIA_ID_PLAYLISTS = "playlists"
        private const val MEDIA_ID_PLAYLIST_PREFIX = "playlist:"
        private const val MEDIA_ID_UNAVAILABLE_PREFIX = "unavailable:"
        private const val RANDOM_SONG_COUNT = 50
    }
}
