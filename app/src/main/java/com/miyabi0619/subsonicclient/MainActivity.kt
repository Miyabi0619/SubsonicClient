package com.miyabi0619.subsonicclient

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.miyabi0619.subsonicclient.data.prefs.CredentialsStore
import com.miyabi0619.subsonicclient.data.repository.LoginRepository
import com.miyabi0619.subsonicclient.ui.home.HomeScreen
import com.miyabi0619.subsonicclient.ui.library.LibraryScreen
import com.miyabi0619.subsonicclient.ui.login.LoginScreen
import com.miyabi0619.subsonicclient.ui.login.LoginViewModel
import com.miyabi0619.subsonicclient.ui.nav.AppDestinations
import com.miyabi0619.subsonicclient.ui.search.SearchScreen
import com.miyabi0619.subsonicclient.ui.settings.SettingsScreen
import com.miyabi0619.subsonicclient.ui.theme.SubsonicClientTheme
import com.miyabi0619.subsonicclient.player.PlayerViewModel
import com.miyabi0619.subsonicclient.ui.player.PlayerBar
import com.miyabi0619.subsonicclient.ui.eq.EqScreen
import com.miyabi0619.subsonicclient.ui.album.AlbumDetailScreen
import com.miyabi0619.subsonicclient.ui.artist.ArtistDetailScreen
import com.miyabi0619.subsonicclient.ui.playlist.PlaylistDetailScreen
import com.miyabi0619.subsonicclient.ui.player.NowPlayingScreen
import com.miyabi0619.subsonicclient.ui.genre.GenreDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubsonicClientTheme {
                SubsonicClientApp()
            }
        }
    }
}

@Composable
fun SubsonicClientApp() {
    val context = LocalContext.current
    val credentialsStore = remember { CredentialsStore(context.applicationContext) }
    val loginRepository = remember { LoginRepository(credentialsStore) }
    val credentials by loginRepository.credentials.collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    if (credentials == null) {
        val loginViewModel: LoginViewModel = viewModel(
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(loginRepository) as T
                }
            }
        )
        LoginScreen(viewModel = loginViewModel)
    } else {
        MainScreen(
            loginRepository = loginRepository,
            onLogout = { scope.launch { loginRepository.logout() } }
        )
    }
}

@Composable
fun MainScreen(
    loginRepository: LoginRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val playerViewModel: PlayerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PlayerViewModel(app) as T
            }
        }
    )
    val playbackState by playerViewModel.playbackState.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val onPlaySong: (String, String?, String?, List<String>) -> Unit = { songId, title, artist, queueIds ->
        playerViewModel.play(songId, queueIds, title, artist)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                PlayerBar(
                    playbackState = playbackState,
                    onPlayPause = { playerViewModel.playPause() },
                    onClick = { navController.navigate("nowplaying") }
                )
                NavigationBar {
                    AppDestinations.entries.forEach { dest ->
                        NavigationBarItem(
                            icon = { Icon(dest.icon, contentDescription = dest.title) },
                            label = { Text(dest.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(AppDestinations.Home.route) {
                HomeScreen(
                    loginRepository = loginRepository,
                    onPlaySong = onPlaySong,
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") }
                )
            }
            composable(AppDestinations.Library.route) {
                LibraryScreen(
                    loginRepository = loginRepository,
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onArtistClick = { artistId -> navController.navigate("artist/$artistId") },
                    onPlaylistClick = { playlistId -> navController.navigate("playlist/$playlistId") },
                    onGenreClick = { genreName ->
                        navController.navigate("genre/${java.net.URLEncoder.encode(genreName, "UTF-8")}")
                    }
                )
            }
            composable(AppDestinations.Search.route) {
                SearchScreen(
                    loginRepository = loginRepository,
                    onPlaySong = onPlaySong,
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") },
                    onArtistClick = { artistId -> navController.navigate("artist/$artistId") },
                    onPlaylistClick = { playlistId -> navController.navigate("playlist/$playlistId") }
                )
            }
            composable(AppDestinations.Settings.route) {
                SettingsScreen(
                    onLogout = onLogout,
                    onOpenEq = { navController.navigate("eq") }
                )
            }
            composable("eq") {
                EqScreen(onBack = { navController.popBackStack() })
            }
            composable("album/{albumId}") { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
                AlbumDetailScreen(
                    albumId = albumId,
                    loginRepository = loginRepository,
                    onBack = { navController.popBackStack() },
                    onPlaySong = onPlaySong
                )
            }
            composable("nowplaying") {
                NowPlayingScreen(
                    playerViewModel = playerViewModel,
                    loginRepository = loginRepository,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("artist/{artistId}") { backStackEntry ->
                val artistId = backStackEntry.arguments?.getString("artistId") ?: return@composable
                ArtistDetailScreen(
                    artistId = artistId,
                    loginRepository = loginRepository,
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { albumId -> navController.navigate("album/$albumId") }
                )
            }
            composable("playlist/{playlistId}") { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    loginRepository = loginRepository,
                    onBack = { navController.popBackStack() },
                    onPlaySong = onPlaySong
                )
            }
            composable("genre/{genreName}") { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("genreName") ?: return@composable
                val genreName = java.net.URLDecoder.decode(encoded, "UTF-8")
                GenreDetailScreen(
                    genreName = genreName,
                    loginRepository = loginRepository,
                    onBack = { navController.popBackStack() },
                    onPlaySong = onPlaySong
                )
            }
        }
    }
}
