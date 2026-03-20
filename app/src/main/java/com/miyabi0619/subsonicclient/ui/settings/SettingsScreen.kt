package com.miyabi0619.subsonicclient.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.data.prefs.AppSettingsStore

private val bitrateOptions = listOf(
    0 to "制限なし（オリジナル）",
    128 to "128 kbps",
    192 to "192 kbps",
    320 to "320 kbps"
)

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onOpenEq: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettingsStore = remember { AppSettingsStore(context.applicationContext) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(appSettingsStore) as T
            }
        }
    )
    val maxBitRate by settingsViewModel.maxBitRate.collectAsState()
    var bitrateExpanded by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "設定", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // イコライザ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenEq)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "イコライザ", style = MaterialTheme.typography.bodyLarge)
            }

            HorizontalDivider()

            // 音質（ビットレート）設定
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bitrateExpanded = !bitrateExpanded }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ストリーミング音質", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = bitrateOptions.find { it.first == maxBitRate }?.second ?: "制限なし",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bitrateExpanded) {
                Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                    bitrateOptions.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setMaxBitRate(value)
                                    bitrateExpanded = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = maxBitRate == value,
                                onClick = {
                                    settingsViewModel.setMaxBitRate(value)
                                    bitrateExpanded = false
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Button(onClick = onLogout) {
                    Text("ログアウト")
                }
            }
        }
    }
}
