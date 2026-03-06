package com.miyabi0619.subsonicclient.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    Home("home", "ホーム", Icons.Default.Home),
    Library("library", "ライブラリ", Icons.AutoMirrored.Filled.List),
    Search("search", "検索", Icons.Default.Search),
    Settings("settings", "設定", Icons.Default.Settings)
}
