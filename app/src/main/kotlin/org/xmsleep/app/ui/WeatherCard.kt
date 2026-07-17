package org.xmsleep.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioCacheManager
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.quote.HealingQuoteManager
import org.xmsleep.app.weather.WeatherData
import org.xmsleep.app.weather.WeatherSoundMapper
import org.xmsleep.app.weather.WeatherCodeMapper

/**
 * 天气智能推荐卡片
 * 当 currentWeather 为 null 时显示加载占位状态
 */
@Composable
fun WeatherCard(
    currentWeather: WeatherData?,
    remoteSounds: List<SoundMetadata>,
    context: android.content.Context,
    modifier: Modifier = Modifier,
    onQuoteClick: () -> Unit = {}
) {
    // 加载中占位状态
    if (currentWeather == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = context.getString(R.string.weather_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val scope = rememberCoroutineScope()

    val weatherCode = currentWeather.weatherCode
    val recommendedSoundIds = remember(weatherCode) {
        WeatherSoundMapper.getRecommendedSoundIds(context, weatherCode)
    }
    val recommendedSounds = remember(recommendedSoundIds, remoteSounds) {
        remoteSounds.filter { it.id in recommendedSoundIds }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-30).dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 天气大图标 + 播放按钮
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = currentWeather.icon,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
                
                // 播放/停止按钮（只显示图标，在天气图标右下角）
                if (recommendedSounds.isNotEmpty()) {
                    val audioManager = remember { AudioManager.getInstance() }
                    val resourceManager = remember { AudioResourceManager.getInstance(context) }
                    val cacheManager = remember { AudioCacheManager.getInstance(context) }
                    var isDownloading by remember { mutableStateOf(false) }
                    var hasStartedPlaying by remember { mutableStateOf(false) }
                    
                    var isAnyPlaying by remember { mutableStateOf(false) }
                    LaunchedEffect(recommendedSounds) {
                        while (true) {
                            isAnyPlaying = recommendedSounds.any { sound ->
                                audioManager.isPlayingRemoteSound(sound.id)
                            }
                            delay(500)
                        }
                    }
                    
                    FilledIconButton(
                        onClick = {
                            if (isAnyPlaying || hasStartedPlaying) {
                                audioManager.stopAllSounds()
                                hasStartedPlaying = false
                            } else if (!isDownloading) {
                                hasStartedPlaying = true
                                scope.launch {
                                    isDownloading = true
                                    try {
                                        recommendedSounds.forEach { sound ->
                                            val cached = cacheManager.getCachedFile(sound.id)
                                            if (cached == null || !cached.exists()) {
                                                withContext(Dispatchers.IO) {
                                                    resourceManager.ensureSoundDownloaded(sound)
                                                }
                                            }
                                        }
                                        recommendedSounds.forEach { sound ->
                                            val uri = withContext(Dispatchers.IO) {
                                                resourceManager.getSoundUri(sound)
                                            }
                                            uri?.let { audioManager.playRemoteSound(context, sound, it) }
                                        }
                                    } catch (e: Exception) {
                                        hasStartedPlaying = false
                                        android.widget.Toast.makeText(
                                            context,
                                            "${context.getString(R.string.download_failed)}: ${e.message}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = 10.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (isAnyPlaying || hasStartedPlaying) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 温度和天气描述
            val weatherDescription = WeatherCodeMapper.toDescription(currentWeather.weatherCode, context)
            Text(
                text = "${currentWeather.temperature.toInt()}°C · $weatherDescription",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            // 湿度和体感温度
            if (currentWeather.humidity > 0 || currentWeather.feelsLike != currentWeather.temperature) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentWeather.humidity > 0) {
                        Text(
                            text = "${context.getString(R.string.humidity)} ${currentWeather.humidity}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    if (currentWeather.feelsLike != currentWeather.temperature) {
                        Text(
                            text = "${context.getString(R.string.feels_like)} ${currentWeather.feelsLike.toInt()}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // 日期显示
            val currentDate = remember {
                val locale = org.xmsleep.app.i18n.LanguageManager.getCurrentLocale(context)
                val pattern = when (locale.language) {
                    "zh" -> "M月d日 EEEE"
                    "ja" -> "M月d日(E)"
                    "ko" -> "M월 d일 EEEE"
                    "ru" -> "d MMMM, EEEE"
                    else -> "EEE, MMM d"
                }
                val sdf = java.text.SimpleDateFormat(pattern, locale)
                sdf.format(java.util.Date())
            }
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 标语
            val weatherQuote = remember { 
                HealingQuoteManager.getRandomQuote(context) 
            }
            Text(
                text = weatherQuote,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onQuoteClick() }
            )
            
            // 推荐音频区域 - 展示所有推荐音频名称（可横向滚动）
            if (recommendedSounds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // 展示所有推荐音频的名称标签 - 横向滚动
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recommendedSounds.forEach { sound ->
                        val soundNameResId = WeatherSoundMapper.getSoundNameResId(sound.id)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = context.getString(soundNameResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
