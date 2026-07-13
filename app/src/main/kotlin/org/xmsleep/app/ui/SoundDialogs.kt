package org.xmsleep.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.components.WheelPicker

/**
 * 音量调节对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeDialog(
    sound: AudioManager.Sound,
    currentVolume: Float,
    onDismiss: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val context = LocalContext.current
    var volume by remember { mutableStateOf(currentVolume) }

    val soundName = when (sound) {
        AudioManager.Sound.UMBRELLA_RAIN -> context.getString(R.string.sound_umbrella_rain)
        AudioManager.Sound.ROWING -> context.getString(R.string.sound_rowing)
        AudioManager.Sound.OFFICE -> context.getString(R.string.sound_office)
        AudioManager.Sound.LIBRARY -> context.getString(R.string.sound_library)
        AudioManager.Sound.HEAVY_RAIN -> context.getString(R.string.sound_heavy_rain)
        AudioManager.Sound.TYPEWRITER -> context.getString(R.string.sound_typewriter)
        AudioManager.Sound.THUNDER -> context.getString(R.string.sound_thunder)
        AudioManager.Sound.CLOCK -> context.getString(R.string.sound_clock)
        AudioManager.Sound.FOREST_BIRDS -> context.getString(R.string.sound_forest_birds)
        AudioManager.Sound.DRIFTING -> context.getString(R.string.sound_drifting)
        AudioManager.Sound.CAMPFIRE -> context.getString(R.string.sound_campfire)
        AudioManager.Sound.WIND -> context.getString(R.string.sound_wind)
        AudioManager.Sound.KEYBOARD -> context.getString(R.string.sound_keyboard)
        AudioManager.Sound.SNOW_WALKING -> context.getString(R.string.sound_snow_walking)
        else -> context.getString(R.string.sound_default)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(soundName) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    context.getString(R.string.adjust_volume),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            onVolumeChange(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 0f..1f,
                        steps = 19
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok))
            }
        }
    )
}

/**
 * 倒计时设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerDialog(
    onDismiss: () -> Unit,
    onTimerSet: (Int) -> Unit,
    currentTimerMinutes: Int = 0
) {
    val context = LocalContext.current
    val lastTimerMinutes = remember { PreferencesManager.getLastTimerMinutes(context) }
    val initialHour = if (currentTimerMinutes > 0) currentTimerMinutes / 60 else (if (lastTimerMinutes > 0) lastTimerMinutes / 60 else 0)
    val initialMinute = if (currentTimerMinutes > 0) currentTimerMinutes % 60 else (if (lastTimerMinutes > 0) lastTimerMinutes % 60 else 30)
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.set_countdown)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentTimerMinutes > 0) {
                    val hours = currentTimerMinutes / 60
                    val mins = currentTimerMinutes % 60
                    val statusText = if (hours > 0) {
                        context.getString(R.string.hours_minutes, hours, if (mins > 0) mins else 0)
                    } else {
                        context.getString(R.string.minutes_only, mins)
                    }
                    Text(
                        text = context.getString(R.string.current_countdown, statusText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (lastTimerMinutes > 0) {
                    val lastHours = lastTimerMinutes / 60
                    val lastMins = lastTimerMinutes % 60
                    val lastText = if (lastHours > 0) {
                        context.getString(R.string.hours_minutes, lastHours, if (lastMins > 0) lastMins else 0)
                    } else {
                        context.getString(R.string.minutes_only, lastMins)
                    }
                    Text(
                        text = context.getString(R.string.last_timer_hint, lastText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    context.getString(R.string.quick_select),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = presetMinutes.chunked(3)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { mins ->
                                FilterChip(
                                    onClick = {
                                        selectedHour = mins / 60
                                        selectedMinute = mins % 60
                                    },
                                    label = { Text(context.getString(R.string.minutes, mins)) },
                                    selected = (selectedHour * 60 + selectedMinute) == mins,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }

                // 自定义时间轮盘选择器
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = context.getString(R.string.hours),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val hourItems = remember { (0..24).map { it.toString().padStart(2, '0') } }
                            WheelPicker(
                                options = hourItems,
                                selectedIndex = selectedHour,
                                onItemSelected = { selectedHour = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = context.getString(R.string.minutes_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }
                            WheelPicker(
                                options = minuteItems,
                                selectedIndex = selectedMinute,
                                onItemSelected = { selectedMinute = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                // 显示当前选择的总时间
                val totalMinutes = selectedHour * 60 + selectedMinute
                Text(
                    text = if (totalMinutes > 0) {
                        if (selectedHour > 0) {
                            context.getString(R.string.hours_minutes, selectedHour, selectedMinute)
                        } else {
                            context.getString(R.string.minutes_only, selectedMinute)
                        }
                    } else {
                        context.getString(R.string.minutes_only, 0)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentTimerMinutes > 0) {
                    TextButton(onClick = { onTimerSet(0); onDismiss() }) {
                        Text(context.getString(R.string.cancel_countdown))
                    }
                }
                TextButton(onClick = {
                    val totalMinutes = selectedHour * 60 + selectedMinute
                    PreferencesManager.saveLastTimerMinutes(context, totalMinutes)
                    onTimerSet(totalMinutes)
                }) {
                    Text(context.getString(R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
