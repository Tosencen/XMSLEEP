package org.xmsleep.app.ui.tomato

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.settings.updateVibrationSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TomatoTimerSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRingtone by remember { mutableStateOf(PreferencesManager.getTomatoRingtone(context)) }
    var pulseEnabled by remember { mutableStateOf(PreferencesManager.getTomatoPulseAnimation(context)) }
    var vibrateEnabled by remember { mutableStateOf(PreferencesManager.getTomatoVibrate(context)) }
    var previewingId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { TomatoRingtonePlayer.stopPreview() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.tomato_settings_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.tomato_ringtone_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    RingtoneOptionRow(
                        label = stringResource(R.string.tomato_ringtone_none),
                        selected = selectedRingtone == TomatoRingtone.NONE,
                        onClick = {
                            selectedRingtone = TomatoRingtone.NONE
                            PreferencesManager.saveTomatoRingtone(context, TomatoRingtone.NONE)
                        }
                    )
                    TomatoRingtone.options.forEach { option ->
                        val label = stringResource(
                            when (option.id) {
                                "ringtone_chime" -> R.string.tomato_ringtone_chime
                                "ringtone_ding" -> R.string.tomato_ringtone_ding
                                "ringtone_marimba" -> R.string.tomato_ringtone_marimba
                                "ringtone_windchime" -> R.string.tomato_ringtone_windchime
                                else -> R.string.tomato_ringtone_chime
                            }
                        )
                        RingtoneOptionRow(
                            label = label,
                            selected = selectedRingtone == option.id,
                            onClick = {
                                selectedRingtone = option.id
                                PreferencesManager.saveTomatoRingtone(context, option.id)
                            },
                            isPreviewing = previewingId == option.id,
                            onPreviewClick = {
                                if (previewingId == option.id) {
                                    TomatoRingtonePlayer.stopPreview()
                                    previewingId = null
                                } else {
                                    TomatoRingtonePlayer.stopPreview()
                                    previewingId = option.id
                                    TomatoRingtonePlayer.preview(context, option.id) {
                                        previewingId = null
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    SettingSwitchRow(
                        label = stringResource(R.string.tomato_pulse_animation),
                        checked = pulseEnabled,
                        onCheckedChange = {
                            pulseEnabled = it
                            PreferencesManager.saveTomatoPulseAnimation(context, it)
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    SettingSwitchRow(
                        label = stringResource(R.string.tomato_vibrate),
                        checked = vibrateEnabled,
                        onCheckedChange = {
                            vibrateEnabled = it
                            PreferencesManager.saveTomatoVibrate(context, it)
                            updateVibrationSetting(context, it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RingtoneOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isPreviewing: Boolean = false,
    onPreviewClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
        if (onPreviewClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onPreviewClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPreviewing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.tomato_ringtone_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isPreviewing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
