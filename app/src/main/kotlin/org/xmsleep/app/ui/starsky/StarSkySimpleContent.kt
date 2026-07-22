package org.xmsleep.app.ui.starsky

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioCacheManager
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.ui.TimerDialog
import org.xmsleep.app.ui.TimerFAB
import org.xmsleep.app.weather.WeatherData
import org.xmsleep.app.weather.WeatherCodeMapper
import org.xmsleep.app.weather.WeatherSoundMapper
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.xmsleep.app.utils.Logger

/**
 * 简化版繁星内容：不分类、直接全量瀑布流展示白噪音，
 * 顶部仅保留标题、倒计时与返回。供设置页“完整版”按钮进入。
 */
@Composable
fun StarSkySimpleContent(
    modifier: Modifier = Modifier,
    onExitSimpleMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLanguage = LanguageManager.getCurrentLanguage(context)
    val audioManager = remember { AudioManager.getInstance() }
    val cacheManager = remember { AudioCacheManager.getInstance(context) }
    val resourceManager = remember { AudioResourceManager.getInstance(context) }

    var remoteSounds by remember {
        mutableStateOf(resourceManager.getCachedManifest()?.sounds ?: emptyList())
    }
    var isLoading by remember { mutableStateOf(remoteSounds.isEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val playingSounds = remember { mutableStateListOf<String>() }
    val downloadingSounds = remember { mutableStateMapOf<String, Float>() }
    val downloadingButNoProgress = remember { mutableStateListOf<String>() }

    // 数据加载（与 StarSkyScreen 同源）
    LaunchedEffect(Unit) {
        if (remoteSounds.isEmpty()) {
            try {
                val manifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (manifest != null) {
                    remoteSounds = manifest.sounds
                } else {
                    remoteSounds = resourceManager.getRemoteSounds()
                }
            } catch (e: Exception) {
                errorMessage = e.message
                if (remoteSounds.isEmpty()) {
                    remoteSounds = resourceManager.getRemoteSounds()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // 定时刷新播放状态
    LaunchedEffect(Unit) {
        while (true) {
            playingSounds.clear()
            playingSounds.addAll(
                remoteSounds.filter { audioManager.isPlayingRemoteSound(it.id) }.map { it.id }
            )
            delay(500)
        }
    }

    val timerManager = remember { TimerManager.getInstance() }
    val isTimerActive by timerManager.isTimerActive.collectAsState()
    val timeLeftMillis by timerManager.timeLeftMillis.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }

    // 右上角天气模块（优先显示缓存，避免依赖网络）
    var currentWeather by remember { mutableStateOf(WeatherSoundMapper.getLastWeather(context)) }

    // 进入简化版时主动取一次天气：有缓存秒显，无缓存则后台刷新（失败保留 null）
    LaunchedEffect(Unit) {
        currentWeather = WeatherSoundMapper.getLastWeather(context)
        val fetched = org.xmsleep.app.ui.fetchAndRefreshWeather(context)
        if (fetched != null) {
            currentWeather = fetched
        }
    }

    // 音量调整弹窗
    var showVolumeDialog by remember { mutableStateOf(false) }
    var selectedSoundForVolume by remember { mutableStateOf<SoundMetadata?>(null) }
    var volume by remember { mutableStateOf(1f) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onExitSimpleMode) {
                Icon(
                    painter = painterResource(R.drawable.low_priority_24),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    context.getString(R.string.simple_mode_full_version),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }

            // 右上角天气模块（紧凑版：图标 + 温度 + 描述）
            currentWeather?.let { weather ->
                val lottieRes = WeatherCodeMapper.toLottieResId(weather.weatherCode, weather.isDay)
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LottieAnimation(
                        composition = composition,
                        iterations = Int.MAX_VALUE,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = "${weather.temperature.toInt()}°",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = WeatherCodeMapper.toDescription(weather.weatherCode, context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            remoteSounds.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(context.getString(R.string.loading), style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> {
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val columnsCount = 3
                val cardWidth = (screenWidth - 32.dp - 12.dp * (columnsCount - 1)) / columnsCount
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(remoteSounds.size) { index ->
                        val sound = remoteSounds[index]
                        Box(modifier = Modifier.width(cardWidth)) {
                            RemoteSoundCard(
                                sound = sound,
                                displayName = sound.getLocalizedName(currentLanguage),
                                isPlaying = playingSounds.contains(sound.id),
                                downloadProgress = downloadingSounds[sound.id],
                                isDownloadingButNoProgress = downloadingButNoProgress.contains(sound.id),
                                columnsCount = columnsCount,
                                onCardClick = {
                                    scope.launch { playSimpleSound(context, sound, audioManager, cacheManager, resourceManager, playingSounds, downloadingSounds, downloadingButNoProgress) }
                                },
                                onVolumeClick = {
                                    selectedSoundForVolume = sound
                                    volume = audioManager.getRemoteVolume(sound.id)
                                    showVolumeDialog = true
                                }
                            )
                        }
                    }
                }
            }
            }
            TimerFAB(
                isTimerActive = isTimerActive,
                timeLeftMillis = timeLeftMillis,
                onClick = { showTimerDialog = true },
                enabled = playingSounds.isNotEmpty() || isTimerActive,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 16.dp)
            )
        }
    }

    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onTimerSet = { minutes ->
                timerManager.startTimer(minutes)
                Toast.makeText(
                    context,
                    context.getString(R.string.countdown_set_minutes, minutes),
                    Toast.LENGTH_SHORT
                ).show()
                showTimerDialog = false
            },
            currentTimerMinutes = if (timerManager.isTimerActive.value) timerManager.getCurrentTimerMinutes() else 0
        )
    }

    if (showVolumeDialog && selectedSoundForVolume != null) {
        val sound = selectedSoundForVolume!!
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text(sound.getLocalizedName(currentLanguage)) },
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
                                audioManager.setRemoteVolume(sound.id, volume)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            valueRange = 0f..1f,
                            steps = 19
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("0%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text("100%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showVolumeDialog = false
                    Toast.makeText(context, context.getString(R.string.volume_adjusted), Toast.LENGTH_SHORT).show()
                }) {
                    Text(context.getString(R.string.ok))
                }
            }
        )
    }
}

private suspend fun playSimpleSound(
    context: android.content.Context,
    sound: SoundMetadata,
    audioManager: AudioManager,
    cacheManager: AudioCacheManager,
    resourceManager: AudioResourceManager,
    playingSounds: MutableList<String>,
    downloadingSounds: MutableMap<String, Float>,
    downloadingButNoProgress: MutableList<String>
) {
    try {
        if (audioManager.isPlayingRemoteSound(sound.id)) {
            audioManager.pauseRemoteSound(sound.id)
            playingSounds.remove(sound.id)
            return
        }
        val cachedFile = cacheManager.getCachedFile(sound.id)
        if (cachedFile == null && sound.remoteUrl != null) {
            downloadingButNoProgress.add(sound.id)
            cacheManager.downloadAudioWithProgress(sound.remoteUrl, sound.id).collect { progress ->
                when (progress) {
                    is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                        downloadingButNoProgress.remove(sound.id)
                        downloadingSounds[sound.id] = progress.bytesRead.toFloat() / progress.contentLength
                    }
                    is org.xmsleep.app.audio.DownloadProgress.Success -> {
                        downloadingButNoProgress.remove(sound.id)
                        downloadingSounds.remove(sound.id)
                        val uri = resourceManager.getSoundUri(sound)
                        if (uri != null) {
                            delay(200)
                            audioManager.playRemoteSound(context, sound, uri)
                            playingSounds.add(sound.id)
                        } else {
                            Toast.makeText(context, context.getString(R.string.play_failed_no_audio_file), Toast.LENGTH_SHORT).show()
                        }
                    }
                    is org.xmsleep.app.audio.DownloadProgress.Error -> {
                        downloadingButNoProgress.remove(sound.id)
                        downloadingSounds.remove(sound.id)
                        Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            val uri = resourceManager.getSoundUri(sound)
            if (uri != null) {
                audioManager.playRemoteSound(context, sound, uri)
                playingSounds.add(sound.id)
            } else {
                Logger.e("StarSkySimpleContent", "无法获取URI: ${sound.id}")
                Toast.makeText(context, context.getString(R.string.play_failed_no_audio_file), Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Logger.e("StarSkySimpleContent", "播放失败: ${e.message}")
        Toast.makeText(context, context.getString(R.string.play_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}
