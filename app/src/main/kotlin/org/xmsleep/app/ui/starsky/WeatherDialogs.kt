package org.xmsleep.app.ui.starsky

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.weather.WeatherCodeMapper
import org.xmsleep.app.weather.WeatherSoundMapper
import org.xmsleep.app.weather.WeatherType

data class DetailedWeatherType(
    val type: WeatherType,
    val labelResId: Int,
    val icon: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherEditDialog(
    context: Context,
    weatherCode: Int,
    onDismiss: () -> Unit,
    onRefreshWeather: () -> Unit
) {
    val ctx = LocalContext.current
    
    // 可用音频列表（按分类组织）
    val availableSoundsMap = mapOf(
        ctx.getString(R.string.weather_category_nature) to listOf("field", "lake", "wind", "birds", "river", "jungle", "campfire", "waves", "night-village", "crickets", "walk-in-snow"),
        ctx.getString(R.string.weather_category_rain) to listOf("rain", "light-rain", "heavy-rain", "drizzle", "thunderstorm", "rain-on-car-roof", "rain-on-umbrella", "rain-on-tent", "rain-on-leaves", "rain-on-raincoat", "rain-on-windowsill", "rain-on-wooden-house", "rain-while-driving", "rain-on-empty-street", "rain-on-eaves", "heavy-rain-on-glass"),
        ctx.getString(R.string.weather_category_other) to listOf("kitchen", "wind-chimes", "light-piano")
    )
    
    var mappings by remember {
        mutableStateOf(WeatherSoundMapper.getMappings(context))
    }
    var selectedWeatherType by remember {
        mutableStateOf(WeatherCodeMapper.toWeatherType(weatherCode))
    }
    
    val detailedWeatherTypes = listOf(
        DetailedWeatherType(WeatherType.SUNNY_CLEAR, R.string.weather_sunny_clear, "☀️"),
        DetailedWeatherType(WeatherType.SUNNY_NIGHT, R.string.weather_sunny_night, "🌙"),
        DetailedWeatherType(WeatherType.CLOUDY_PARTLY, R.string.weather_cloudy_partly, "⛅"),
        DetailedWeatherType(WeatherType.CLOUDY_OVERCAST, R.string.weather_cloudy_overcast, "☁️"),
        DetailedWeatherType(WeatherType.FOGGY, R.string.weather_foggy, "🌫️"),
        DetailedWeatherType(WeatherType.FOGGY_DRIZZLE, R.string.weather_foggy_drizzle, "🌧️"),
        DetailedWeatherType(WeatherType.RAIN_LIGHT, R.string.weather_rain_light, "🌦️"),
        DetailedWeatherType(WeatherType.RAIN_MODERATE, R.string.weather_rain_moderate, "🌧️"),
        DetailedWeatherType(WeatherType.RAIN_HEAVY, R.string.weather_rain_heavy, "⛈️"),
        DetailedWeatherType(WeatherType.RAIN_SHOWER, R.string.weather_rain_shower, "🌧️"),
        DetailedWeatherType(WeatherType.SNOW_LIGHT, R.string.weather_snow_light, "🌨️"),
        DetailedWeatherType(WeatherType.SNOW_MODERATE, R.string.weather_snow_moderate, "❄️"),
        DetailedWeatherType(WeatherType.SNOW_HEAVY, R.string.weather_snow_heavy, "❄️"),
        DetailedWeatherType(WeatherType.SNOW_SLEET, R.string.weather_snow_sleet, "🌨️"),
        DetailedWeatherType(WeatherType.THUNDERSTORM, R.string.weather_thunderstorm, "⛈️"),
        DetailedWeatherType(WeatherType.THUNDERSTORM_HAIL, R.string.weather_thunderstorm_hail, "⛈️")
    )
    
    val currentMapping = remember(selectedWeatherType, mappings) {
        mappings.find { it.weatherTypes.contains(selectedWeatherType) }
    }
    var selectedSoundIdsList by remember(currentMapping) {
        mutableStateOf(currentMapping?.soundIds ?: emptyList())
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ctx.getString(R.string.weather_sound_mapping))
                TextButton(onClick = onRefreshWeather) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                val weatherDescription = WeatherCodeMapper.toDescription(weatherCode, ctx)
                Text(
                    text = "${ctx.getString(R.string.weather_current)} $weatherDescription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 天气类型选择 - 左右滑动
                Text(
                    text = ctx.getString(R.string.weather_select_type),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detailedWeatherTypes.forEach { detailedType ->
                        FilterChip(
                            selected = selectedWeatherType == detailedType.type,
                            onClick = { selectedWeatherType = detailedType.type },
                            label = { 
                                Text("${detailedType.icon} ${ctx.getString(detailedType.labelResId)}") 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 噪音选择
                Text(
                    text = ctx.getString(R.string.weather_select_sound),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    availableSoundsMap.forEach { (category, soundIds) ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        soundIds.forEach { soundId ->
                            val isSelected = selectedSoundIdsList.contains(soundId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSoundIdsList = if (isSelected) {
                                            selectedSoundIdsList - soundId
                                        } else {
                                            selectedSoundIdsList + soundId
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedSoundIdsList = if (checked) {
                                            selectedSoundIdsList + soundId
                                        } else {
                                            selectedSoundIdsList - soundId
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(ctx.getString(WeatherSoundMapper.getSoundNameResId(soundId)))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 保存映射
                    val newMappings = mappings.toMutableList()
                    newMappings.removeAll { it.weatherTypes.contains(selectedWeatherType) }
                    newMappings.add(
                        WeatherSoundMapper.SoundMapping(
                            weatherTypes = listOf(selectedWeatherType),
                            soundIds = selectedSoundIdsList
                        )
                    )
                    WeatherSoundMapper.saveMappings(context, newMappings)
                    onDismiss()
                }
            ) {
                Text(ctx.getString(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ctx.getString(R.string.cancel))
            }
        }
    )
}

@Composable
fun ManualWeatherSelectorDialog(
    onDismiss: () -> Unit,
    onWeatherSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val weatherOptions = listOf(
        0 to context.getString(R.string.weather_option_sunny),
        1 to context.getString(R.string.weather_option_partly_cloudy),
        2 to context.getString(R.string.weather_option_cloudy),
        3 to context.getString(R.string.weather_option_overcast),
        45 to context.getString(R.string.weather_option_foggy),
        51 to context.getString(R.string.weather_option_light_drizzle),
        61 to context.getString(R.string.weather_option_light_rain),
        63 to context.getString(R.string.weather_option_moderate_rain),
        65 to context.getString(R.string.weather_option_heavy_rain),
        71 to context.getString(R.string.weather_option_light_snow),
        73 to context.getString(R.string.weather_option_moderate_snow),
        75 to context.getString(R.string.weather_option_heavy_snow),
        80 to context.getString(R.string.weather_option_shower),
        95 to context.getString(R.string.weather_option_thunderstorm)
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.weather_manual_select)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = context.getString(R.string.weather_location_failed),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                weatherOptions.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onWeatherSelected(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
