@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.miyabi0619.subsonicclient.ui.eq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miyabi0619.subsonicclient.eq.EqPresetType
import com.miyabi0619.subsonicclient.eq.EqStore
import com.miyabi0619.subsonicclient.eq.EqViewModel

@Composable
fun EqScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eqStore = remember { EqStore(context.applicationContext) }
    val viewModel: EqViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EqViewModel(eqStore) as T
            }
        }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("イコライザ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("イコライザ", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "プリセット",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EqPresetType.entries.chunked(3).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.applyPreset(preset) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(preset.displayName)
                            }
                        }
                    }
                }
            }
            androidx.compose.material3.TextButton(
                onClick = viewModel::resetToFlat,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("フラットにリセット")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                "バンド（dB）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (state.bands.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    state.bands.forEachIndexed { index, band ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(140.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = band.gainDb,
                                    onValueChange = { viewModel.setBandGain(index, it) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier
                                        .height(120.dp)
                                        .padding(8.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Text(
                                text = bandLabel(band.centerHz),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${band.gainDb.toInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun bandLabel(hz: Float): String = when {
    hz >= 1000f -> "${(hz / 1000f).toInt()}k"
    else -> hz.toInt().toString()
}
