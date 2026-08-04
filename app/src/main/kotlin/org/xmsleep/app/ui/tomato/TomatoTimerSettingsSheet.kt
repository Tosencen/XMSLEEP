package org.xmsleep.app.ui.tomato

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.settings.updateVibrationSetting

/**
 * 番茄时钟设置弹层：结束铃声选择 + 描边框动画 + 震动开关
 */
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.tomato_settings_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            // 结束铃声
            Text(
                text = stringResource(R.string.tomato_ringtone_label),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // 描边框动画开关
            SettingSwitchRow(
                label = stringResource(R.string.tomato_pulse_animation),
                checked = pulseEnabled,
                onCheckedChange = {
                    pulseEnabled = it
                    PreferencesManager.saveTomatoPulseAnimation(context, it)
                }
            )

            // 震动开关
            SettingSwitchRow(
                label = stringResource(R.string.tomato_vibrate),
                checked = vibrateEnabled,
                onCheckedChange = {
                    vibrateEnabled = it
                    PreferencesManager.saveTomatoVibrate(context, it)
                    // 同步更新通知渠道的震动设置
                    updateVibrationSetting(context, it)
                }
            )
        }
    }
}

@Composable
private fun RingtoneOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
