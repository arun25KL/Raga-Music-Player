package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.SleepTimerOption
import com.example.model.SleepTimerState
import com.example.ui.theme.ArtisticTextPrimary
import com.example.ui.theme.ArtisticTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioFxBottomSheet(
    onDismiss: () -> Unit,
    equalizerState: EqualizerState,
    playbackSpeed: Float,
    sleepTimerState: SleepTimerState,
    crossfadeSeconds: Int,
    isGaplessEnabled: Boolean,
    isReplayGainEnabled: Boolean,
    superBoostPercent: Int,
    onToggleEqualizer: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandLevelChanged: (Int, Int) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onPlaybackSpeedChanged: (Float) -> Unit,
    onSelectSleepTimer: (SleepTimerOption) -> Unit,
    onCrossfadeChanged: (Int) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onToggleReplayGain: (Boolean) -> Unit,
    onSuperBoostChanged: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.background

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(borderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Audio FX & Sound Enhancer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                        Text(
                            text = "Equalizer, Sound Boost, Crossfade & Effects",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryColor
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_fx_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Audio Playback & Ergonomics: Crossfade, Gapless, ReplayGain
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Audio Ergonomics & Normalization",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Crossfade Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Track Crossfade",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (crossfadeSeconds > 0) "${crossfadeSeconds}s" else "Off (Gapless)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Slider(
                        value = crossfadeSeconds.toFloat(),
                        onValueChange = { onCrossfadeChanged(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = borderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("slider_crossfade")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 200% Super Boost & Loudness Enhancer Slider with Peak Limiter Warning
                    val isPeakGain = superBoostPercent > 140
                    val boostAccentColor = if (superBoostPercent > 160) Color(0xFFEF4444) else if (isPeakGain) Color(0xFFF59E0B) else primaryColor

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "200% Super Boost",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isPeakGain) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = boostAccentColor.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, boostAccentColor.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = boostAccentColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "PEAK GAIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = boostAccentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (superBoostPercent > 140) {
                                    "⚠️ High Hardware Gain (+${(superBoostPercent - 100) * 20}mB) • Keep volume safe to prevent clipping"
                                } else if (superBoostPercent > 100) {
                                    "+${(superBoostPercent - 100) * 20}mB hardware gain active"
                                } else {
                                    "Standard volume output (0 dB)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPeakGain) boostAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (superBoostPercent > 100) boostAccentColor.copy(alpha = 0.2f) else containerColor,
                            border = if (isPeakGain) BorderStroke(1.dp, boostAccentColor.copy(alpha = 0.5f)) else null
                        ) {
                            Text(
                                text = "$superBoostPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (superBoostPercent > 100) boostAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = superBoostPercent.toFloat(),
                        onValueChange = { onSuperBoostChanged(it.toInt()) },
                        valueRange = 100f..200f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = boostAccentColor,
                            activeTrackColor = boostAccentColor,
                            inactiveTrackColor = borderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .testTag("slider_super_boost")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ReplayGain / Volume Normalization Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Volume Normalization (ReplayGain)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Balances loudness levels between tracks automatically",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isReplayGainEnabled,
                            onCheckedChange = onToggleReplayGain,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = containerColor
                            ),
                            modifier = Modifier.testTag("switch_replay_gain")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Gapless Playback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gapless Playback",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Instant zero-delay transition between consecutive tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = isGaplessEnabled,
                            onCheckedChange = onToggleGapless,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = containerColor
                            ),
                            modifier = Modifier.testTag("switch_gapless")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Playback Speed Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Playback Speed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(speeds) { speed ->
                            val isSelected = playbackSpeed == speed
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) primaryColor else containerColor,
                                border = BorderStroke(1.dp, if (isSelected) primaryColor else borderColor),
                                modifier = Modifier
                                    .clickable { onPlaybackSpeedChanged(speed) }
                                    .testTag("btn_speed_$speed")
                            ) {
                                Text(
                                    text = "${speed}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Sleep Timer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sleep Timer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }
                        if (sleepTimerState.isRunning) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = primaryColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "⏳ ${sleepTimerState.remainingFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SleepTimerOption.values()) { opt ->
                            val isSelected = sleepTimerState.selectedOption == opt
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) primaryColor else containerColor,
                                border = BorderStroke(1.dp, if (isSelected) primaryColor else borderColor),
                                modifier = Modifier
                                    .clickable { onSelectSleepTimer(opt) }
                                    .testTag("btn_timer_${opt.name}")
                            ) {
                                Text(
                                    text = opt.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. 5-Band Equalizer & Bass Boost Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "5-Band Equalizer & Bass",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        }

                        Switch(
                            checked = equalizerState.isEnabled,
                            onCheckedChange = onToggleEqualizer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = containerColor
                            ),
                            modifier = Modifier.testTag("switch_equalizer")
                        )
                    }

                    if (equalizerState.isEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Presets Row
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(EqualizerPreset.values()) { preset ->
                                val isSelected = equalizerState.currentPreset == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectPreset(preset) },
                                    label = { Text(preset.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = containerColor,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp).testTag("chip_preset_${preset.name}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bass Boost Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bass Boost",
                                style = MaterialTheme.typography.labelMedium,
                                color = secondaryColor
                            )
                            Text(
                                text = "${equalizerState.bassBoostStrength / 10}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }

                        Slider(
                            value = equalizerState.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChanged(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = borderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .testTag("slider_bass_boost")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3D Virtualizer Surround Sound Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3D Spatial Virtualizer",
                                style = MaterialTheme.typography.labelMedium,
                                color = secondaryColor
                            )
                            Text(
                                text = "${equalizerState.virtualizerStrength / 10}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }

                        Slider(
                            value = equalizerState.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerChanged(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = secondaryColor,
                                activeTrackColor = secondaryColor,
                                inactiveTrackColor = borderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .testTag("slider_virtualizer")
                        )

                        // Equalizer Bands Sliders
                        if (equalizerState.bands.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            equalizerState.bands.forEach { band ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = band.displayFrequency,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryColor,
                                        modifier = Modifier.width(60.dp)
                                    )
                                    Slider(
                                        value = band.levelMb.toFloat(),
                                        onValueChange = { onBandLevelChanged(band.index, it.toInt()) },
                                        valueRange = band.minMb.toFloat()..band.maxMb.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = primaryColor,
                                            activeTrackColor = primaryColor,
                                            inactiveTrackColor = borderColor
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(24.dp)
                                            .testTag("slider_band_${band.index}")
                                    )
                                    Text(
                                        text = "${band.levelMb / 100} dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        modifier = Modifier.width(46.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
