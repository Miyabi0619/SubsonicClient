package com.miyabi0619.subsonicclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            onLogout = { scope.launch { loginRepository.logout() } }
        )
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable(AppDestinations.Home.route) { HomeScreen() }
            composable(AppDestinations.Library.route) { LibraryScreen() }
            composable(AppDestinations.Search.route) { SearchScreen() }
            composable(AppDestinations.Settings.route) {
                SettingsScreen(onLogout = onLogout)
            }
        }
    }
}
