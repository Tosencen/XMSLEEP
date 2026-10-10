package org.xmsleep.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.components.FrostedGlassSheet

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

    FrostedGlassSheet(
        visible = true,
        onDismissRequest = onDismiss
    ) {
        Text(
            soundName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 12.dp)
        ) {
            Text(context.getString(R.string.ok))
        }
    }
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
    // M3 圆盘时间选择器（24 小时制）：小时 0–23、分钟 0–59
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )
    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)

    FrostedGlassSheet(
        visible = true,
        onDismissRequest = onDismiss
    ) {
        Text(
            context.getString(R.string.set_countdown),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentTimerMinutes > 0) {
                val hours = currentTimerMinutes / 60
                val mins = currentTimerMinutes % 60
                val statusText = if (hours > 0) {
                    context.resources.getQuantityString(R.plurals.hours_minutes, hours, hours, if (mins > 0) mins else 0)
                } else {
                    context.resources.getQuantityString(R.plurals.minutes_only, mins, mins)
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
                    context.resources.getQuantityString(R.plurals.hours_minutes, lastHours, lastHours, if (lastMins > 0) lastMins else 0)
                } else {
                    context.resources.getQuantityString(R.plurals.minutes_only, lastMins, lastMins)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetMinutes.forEach { mins ->
                    FilterChip(
                        onClick = {
                            timePickerState.hour = mins / 60
                            timePickerState.minute = mins % 60
                        },
                        label = { Text(context.resources.getQuantityString(R.plurals.minutes, mins, mins)) },
                        selected = (timePickerState.hour * 60 + timePickerState.minute) == mins
                    )
                }
            }

            // M3 圆盘时间选择器（小时/分钟同一圆盘，点击中央数字切换模式）
            TimePicker(state = timePickerState)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
            if (currentTimerMinutes > 0) {
                TextButton(onClick = { onTimerSet(0); onDismiss() }) {
                    Text(context.getString(R.string.cancel_countdown))
                }
            }
            TextButton(onClick = {
                val totalMinutes = timePickerState.hour * 60 + timePickerState.minute
                PreferencesManager.saveLastTimerMinutes(context, totalMinutes)
                onTimerSet(totalMinutes)
            }) {
                Text(context.getString(R.string.ok))
            }
        }
    }
}
