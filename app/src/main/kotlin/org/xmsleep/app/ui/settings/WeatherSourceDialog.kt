package org.xmsleep.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.weather.WeatherService
import org.xmsleep.app.weather.WeatherSourceConfig
import org.xmsleep.app.weather.WeatherSoundMapper

/**
 * 天气数据源设置弹窗：用户填写自己的和风天气 Host + Key（BYOK）。
 * 未配置时 App 回退到 Open-Meteo（免费、无需 Key）。
 */
@Composable
fun WeatherSourceDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var host by remember { mutableStateOf(WeatherSourceConfig.getHost(context)) }
    var key by remember { mutableStateOf(WeatherSourceConfig.getKey(context)) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var showTutorial by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weather_source_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.weather_source_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.weather_source_host)) },
                    placeholder = { Text(stringResource(R.string.weather_source_host_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.weather_source_key)) },
                    placeholder = { Text(stringResource(R.string.weather_source_key_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            testing = true
                            status = null
                            val r = WeatherService().validateQWeather(host, key)
                            status = if (r.isSuccess) {
                                context.getString(R.string.weather_source_test_ok)
                            } else {
                                context.getString(R.string.weather_source_test_fail, r.exceptionOrNull()?.message ?: "")
                            }
                            testing = false
                        }
                    },
                    enabled = !testing && host.isNotBlank() && key.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.weather_source_test))
                    }
                }

                status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.startsWith("✓")) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                TextButton(
                    onClick = { showTutorial = !showTutorial },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.weather_source_tutorial))
                }

                if (showTutorial) {
                    Text(
                        stringResource(R.string.weather_source_tutorial_steps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.qweather.com/"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.open_failed), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.weather_source_apply_link))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    WeatherSourceConfig.save(context, host, key)
                    WeatherSoundMapper.clearLastWeather(context)
                    Toast.makeText(context, context.getString(R.string.weather_source_save), Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                enabled = host.isNotBlank() && key.isNotBlank()
            ) {
                Text(stringResource(R.string.weather_source_save))
            }
        },
        dismissButton = {
            Row {
                if (WeatherSourceConfig.isConfigured(context)) {
                    TextButton(
                        onClick = {
                            WeatherSourceConfig.clear(context)
                            WeatherSoundMapper.clearLastWeather(context)
                            host = ""
                            key = ""
                            status = null
                            Toast.makeText(context, context.getString(R.string.weather_source_cleared), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(stringResource(R.string.weather_source_clear))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.weather_source_cancel))
                }
            }
        }
    )
}
