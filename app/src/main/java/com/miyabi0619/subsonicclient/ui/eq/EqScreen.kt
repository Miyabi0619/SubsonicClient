@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.miyabi0619.subsonicclient.ui.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
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
            val hwStatus = state.hardwareAvailable
            if (hwStatus != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hwStatus) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            "EQが端末に適用されています",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            "この端末ではハードウェアEQが利用できません",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
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
                val minDb = -12f
                val maxDb = 12f
                val majorTicks = listOf(12f, 6f, 0f, -6f, -12f)
                val bandSectionHeight = 240.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    DbRuler(
                        majorTicks = majorTicks,
                        valueRange = minDb..maxDb,
                        modifier = Modifier
                            .height(bandSectionHeight)
                            .width(44.dp)
                            .padding(end = 4.dp)
                    )
                    state.bands.forEachIndexed { index, band ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(56.dp)
                        ) {
                            VerticalGainSlider(
                                value = band.gainDb,
                                onValueChange = { viewModel.setBandGain(index, it) },
                                valueRange = minDb..maxDb,
                                tickValues = majorTicks,
                                modifier = Modifier
                                    .height(bandSectionHeight)
                                    .width(32.dp)
                                    .padding(horizontal = 2.dp),
                                enabled = state.enabled
                            )
                            Text(
                                text = bandLabel(band.centerHz),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = formatDb(band.gainDb),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DbRuler(
    majorTicks: List<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val density = LocalDensity.current
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackX = size.width - with(density) { 10.dp.toPx() }
                majorTicks.forEach { v ->
                    val frac = ((valueRange.endInclusive - v) / range).coerceIn(0f, 1f)
                    val y = size.height * frac
                    drawLine(
                        color = outlineVariant,
                        start = Offset(trackX - with(density) { 10.dp.toPx() }, y),
                        end = Offset(trackX, y),
                        strokeWidth = with(density) { 1.dp.toPx() }
                    )
                }
                // 縦の基準線
                drawLine(
                    color = outlineVariant,
                    start = Offset(trackX, 0f),
                    end = Offset(trackX, size.height),
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
            }

            majorTicks.forEach { v ->
                val frac = ((valueRange.endInclusive - v) / range).coerceIn(0f, 1f)
                val yPx = heightPx * frac
                val yDp = with(density) { yPx.toDp() }
                Text(
                    text = v.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .padding(top = (yDp - 8.dp).coerceAtLeast(0.dp))
                )
            }
        }
    }
}

@Composable
private fun VerticalGainSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    tickValues: List<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(enabled, valueRange.start, valueRange.endInclusive) {
                if (!enabled) return@pointerInput
                fun yToValue(y: Float, height: Float, topPad: Float, bottomPad: Float): Float {
                    val trackTop = topPad
                    val trackBottom = height - bottomPad
                    val clampedY = y.coerceIn(trackTop, trackBottom)
                    val frac = 1f - ((clampedY - trackTop) / (trackBottom - trackTop).coerceAtLeast(1f))
                    return (valueRange.start + frac * (valueRange.endInclusive - valueRange.start))
                        .coerceIn(valueRange.start, valueRange.endInclusive)
                }

                detectTapGestures { offset ->
                    val v = yToValue(
                        y = offset.y,
                        height = size.height.toFloat(),
                        topPad = 12f,
                        bottomPad = 12f
                    )
                    onValueChange(v)
                }
            }
            .pointerInput(enabled, valueRange.start, valueRange.endInclusive) {
                if (!enabled) return@pointerInput
                fun yToValue(y: Float, height: Float, topPad: Float, bottomPad: Float): Float {
                    val trackTop = topPad
                    val trackBottom = height - bottomPad
                    val clampedY = y.coerceIn(trackTop, trackBottom)
                    val frac = 1f - ((clampedY - trackTop) / (trackBottom - trackTop).coerceAtLeast(1f))
                    return (valueRange.start + frac * (valueRange.endInclusive - valueRange.start))
                        .coerceIn(valueRange.start, valueRange.endInclusive)
                }

                detectVerticalDragGestures { change, _ ->
                    val v = yToValue(
                        y = change.position.y,
                        height = size.height.toFloat(),
                        topPad = 12f,
                        bottomPad = 12f
                    )
                    onValueChange(v)
                    change.consume()
                }
            }
    ) {
        val density = LocalDensity.current
        val outlineVariant = MaterialTheme.colorScheme.outlineVariant
        val onSurface = MaterialTheme.colorScheme.onSurface
        val primary = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outline
        val surface = MaterialTheme.colorScheme.surface
        val min = valueRange.start
        val max = valueRange.endInclusive
        val range = (max - min).coerceAtLeast(1e-6f)

        fun valueToY(v: Float, height: Float, topPad: Float, bottomPad: Float): Float {
            val frac = ((max - v) / range).coerceIn(0f, 1f)
            val trackTop = topPad
            val trackBottom = height - bottomPad
            return trackTop + (trackBottom - trackTop) * frac
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val topPad = with(density) { 12.dp.toPx() }
            val bottomPad = with(density) { 12.dp.toPx() }
            val trackX = size.width / 2f
            val trackTop = topPad
            val trackBottom = size.height - bottomPad
            val thumbY = valueToY(value, size.height, topPad, bottomPad)
            val zeroY = if (0f in min..max) valueToY(0f, size.height, topPad, bottomPad) else null

            // 背景トラック
            drawLine(
                color = outlineVariant,
                start = Offset(trackX, trackTop),
                end = Offset(trackX, trackBottom),
                strokeWidth = with(density) { 4.dp.toPx() }
            )

            // 0dB基準線（見やすさ重視）
            if (zeroY != null) {
                drawLine(
                    color = onSurface.copy(alpha = 0.35f),
                    start = Offset(trackX - with(density) { 12.dp.toPx() }, zeroY),
                    end = Offset(trackX + with(density) { 12.dp.toPx() }, zeroY),
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
            }

            // アクティブ区間（0dBから現在値まで）
            if (zeroY != null) {
                val startY = zeroY
                val endY = thumbY
                drawLine(
                    color = primary,
                    start = Offset(trackX, startY),
                    end = Offset(trackX, endY),
                    strokeWidth = with(density) { 4.dp.toPx() }
                )
            } else {
                drawLine(
                    color = primary,
                    start = Offset(trackX, trackBottom),
                    end = Offset(trackX, thumbY),
                    strokeWidth = with(density) { 4.dp.toPx() }
                )
            }

            // 目盛り（tick）
            tickValues.forEach { v ->
                val y = valueToY(v, size.height, topPad, bottomPad)
                drawLine(
                    color = outlineVariant,
                    start = Offset(trackX + with(density) { 10.dp.toPx() }, y),
                    end = Offset(trackX + with(density) { 16.dp.toPx() }, y),
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
            }

            // サム
            drawCircle(
                color = if (enabled) primary else outline,
                radius = with(density) { 8.dp.toPx() },
                center = Offset(trackX, thumbY)
            )
            drawCircle(
                color = surface,
                radius = with(density) { 4.dp.toPx() },
                center = Offset(trackX, thumbY)
            )
        }
    }
}

private fun formatDb(db: Float): String {
    val rounded = (kotlin.math.round(db * 2f) / 2f)
    return when {
        rounded > 0f -> "+${rounded}dB"
        rounded < 0f -> "${rounded}dB"
        else -> "0dB"
    }
}

private fun bandLabel(hz: Float): String = when {
    hz >= 1000f -> "${(hz / 1000f).toInt()}k"
    else -> hz.toInt().toString()
}
