package org.xmsleep.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.Constants
import org.xmsleep.app.ui.components.AboutDialog
import org.xmsleep.app.ui.components.BackgroundSelectionDialog
import org.xmsleep.app.ui.components.ClearCacheDialog
import org.xmsleep.app.ui.components.LanguageSelectionDialog
import org.xmsleep.app.ui.components.SwitchItem
import org.xmsleep.app.ui.BackgroundSelection
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.update.UpdateDialog
import org.xmsleep.app.ui.starsky.WeatherEditDialog
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.*

/**
 * 设置页面 - 应用配置和管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = true,
    onHideAnimationChange: (Boolean) -> Unit = {},
    backgroundSelection: BackgroundSelection = BackgroundSelection.None,
    onBackgroundSelectionChange: (BackgroundSelection) -> Unit = {},
    updateViewModel: org.xmsleep.app.update.UpdateViewModel,
    currentLanguage: LanguageManager.Language,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToSounds: () -> Unit = {},
    onNavigateToQuoteHistory: () -> Unit = {},
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<AudioManager.Sound>>,
    locationPermissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    onScrollDetected: () -> Unit = {},
    onContentHiddenChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    val timerManager = remember { org.xmsleep.app.timer.TimerManager.getInstance() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(0L) }
    var isCalculatingCache by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showAutoCountdownDialog by remember { mutableStateOf(false) }
    var autoCountdownMinutes by remember { 
        mutableIntStateOf(org.xmsleep.app.preferences.PreferencesManager.getAutoCountdownMinutes(context))
    }
    
    // 天气智能推荐状态
    var weatherEnabled by remember { mutableStateOf(org.xmsleep.app.weather.WeatherSoundMapper.isEnabled(context)) }
    var hasLocationPermission by remember { 
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showWeatherEditDialog by remember { mutableStateOf(false) }
    var currentWeatherCodeForDialog by remember { mutableIntStateOf(0) }
    
    // 天气开关处理
    val onWeatherToggle: (Boolean) -> Unit = { enabled ->
        weatherEnabled = enabled
        org.xmsleep.app.weather.WeatherSoundMapper.setEnabled(context, enabled)
        if (enabled && !hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    
    // 定期检查权限状态
    LaunchedEffect(Unit) {
        hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    val updateState by updateViewModel.updateState.collectAsState()
    // 获取当前版本号
    val currentVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    // 实时音量状态（用于显示当前音量）
    var currentVolumeDisplay by remember { mutableStateOf(0f) }
    
    // 定期更新缓存大小和音量显示
    LaunchedEffect(Unit) {
        while (true) {
            isCalculatingCache = true
            cacheSize = calculateCacheSize(context)
            isCalculatingCache = false
            
            // 更新音量显示：获取第一个正在播放的声音的音量，优先检查本地音频，然后检查远程音频
            val playingSounds = audioManager.getPlayingSounds()
            val playingRemoteSounds = audioManager.getPlayingRemoteSoundIds()
            currentVolumeDisplay = when {
                playingSounds.isNotEmpty() -> {
                    audioManager.getVolume(playingSounds.first())
                }
                playingRemoteSounds.isNotEmpty() -> {
                    audioManager.getRemoteVolume(playingRemoteSounds.first())
                }
                else -> {
                    // 如果没有正在播放的音频，保持当前显示值不变（不重置为0）
                    currentVolumeDisplay
                }
            }
            
            // 检查缓存是否超过200M (200 * 1024 * 1024 字节)
            val thresholdBytes = 200L * 1024 * 1024
            if (cacheSize > thresholdBytes && !isClearingCache) {
                // 自动清理缓存
                isClearingCache = true
                scope.launch {
                    try {
                        clearApplicationCache(context)
                        cacheSize = 0L
                        Toast.makeText(context, context.getString(R.string.cache_auto_cleared), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.auto_clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    } finally {
                        isClearingCache = false
                        // 清理完成后重新计算缓存大小
                        cacheSize = calculateCacheSize(context)
                    }
                }
            }
            
            // 每5秒更新一次缓存大小和音量
            delay(5000)
        }
    }
    
    // 实时监听音量变化（更频繁的更新）
    LaunchedEffect(Unit) {
        while (true) {
            val playingSounds = audioManager.getPlayingSounds()
            val playingRemoteSounds = audioManager.getPlayingRemoteSoundIds()
            // 只在有音频播放时才更新显示，避免覆盖用户设置的值
            when {
                playingSounds.isNotEmpty() -> {
                    currentVolumeDisplay = audioManager.getVolume(playingSounds.first())
                }
                playingRemoteSounds.isNotEmpty() -> {
                    currentVolumeDisplay = audioManager.getRemoteVolume(playingRemoteSounds.first())
                }
                // 如果没有正在播放的音频，保持当前显示值不变
            }
            delay(300) // 每300ms更新一次音量显示
        }
    }
    // 灯泡功能状态
    var isContentHidden by remember { mutableStateOf(false) }
    var showPullRing by remember { mutableStateOf(false) }
    
    // 倒计时状态
    val isTimerActive by timerManager.isTimerActive.collectAsState()
    val timeLeftMillis by timerManager.timeLeftMillis.collectAsState()
    
    // 监听音频播放状态，自动取消倒计时
    LaunchedEffect(Unit) {
        while (true) {
            val hasAnyPlayingSounds = audioManager.hasAnyPlayingSounds()
            
            // 如果没有声音在播放且倒计时是激活状态，自动取消倒计时
            if (!hasAnyPlayingSounds && isTimerActive) {
                timerManager.cancelTimer()
            }
            
            delay(1000) // 每秒检查一次
        }
    }
    
    // 通知 MainScreen 内容隐藏状态变化
    LaunchedEffect(isContentHidden) {
        onContentHiddenChange(isContentHidden)
    }
    
    // 内容透明度动画
    val contentAlpha by animateFloatAsState(
        targetValue = if (isContentHidden) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "content_alpha"
    )
    
    // 拉环掉落动画进度
    val pullRingProgress by animateFloatAsState(
        targetValue = if (showPullRing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pull_ring_progress"
    )
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 主内容层
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha }
                .then(
                    // 当内容隐藏时，禁用所有点击事件
                    if (isContentHidden) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    // 拦截所有触摸事件
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            // 固定标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    context.getString(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        
        // 可滚动内容区域
        val scrollState = rememberScrollState()
        
        // 监听滚动状态，触发浮动按钮收缩
        var previousScrollValue by remember { mutableStateOf(scrollState.value) }
        LaunchedEffect(scrollState.value) {
            if (scrollState.value != previousScrollValue) {
                onScrollDetected()
                previousScrollValue = scrollState.value
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp) // 减少水平padding，与SettingsCategory一致
                .padding(bottom = 140.dp), // 增加底部 padding 避开底部导航栏
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 外观设置
        SettingsCategory(
            title = context.getString(R.string.appearance),
            items = listOf(
                SettingsCategoryItem(
                    icon = Icons.Default.Palette,
                    title = { Text(context.getString(R.string.theme_and_colors)) },
                    description = {
                        Text(
                            context.getString(R.string.appearance_mode_theme_color),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onNavigateToTheme
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.Wallpaper,
                    title = { Text(context.getString(R.string.background_settings)) },
                    description = {
                        Text(
                            context.getString(R.string.select_background),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showBackgroundDialog = true }
                )
            )
        )
        
        Spacer(Modifier.height(8.dp))
        
        // 系统设置
        SettingsCategory(
            title = context.getString(R.string.system),
            items = listOf(
                SettingsCategoryItem(
                    icon = Icons.Default.Translate,
                    title = { Text(context.getString(R.string.language)) },
                    description = {
                        Text(
                            context.getString(R.string.language_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            currentLanguage.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { showLanguageDialog = true }
                ),
                SettingsCategoryItem(
                    icon = Icons.Outlined.Cloud,
                    title = { Text(context.getString(R.string.weather_smart_recommend)) },
                    description = {
                        Text(
                            context.getString(R.string.weather_smart_recommend_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = weatherEnabled,
                            onCheckedChange = onWeatherToggle
                        )
                    },
                    onClick = { onWeatherToggle(!weatherEnabled) }
                ),
                SettingsCategoryItem(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = { Text(context.getString(R.string.adjust_all_volume)) },
                    description = {
                        Text(
                            context.getString(R.string.unified_adjust_all_sound_volume),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            "${(currentVolumeDisplay * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { showVolumeDialog = true }
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.Timer,
                    title = { Text(context.getString(R.string.auto_countdown)) },
                    description = {
                        Text(
                            context.getString(R.string.auto_countdown_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            when (autoCountdownMinutes) {
                                0 -> context.getString(R.string.no_countdown)
                                30 -> context.getString(R.string.countdown_30_minutes)
                                45 -> context.getString(R.string.countdown_45_minutes)
                                60 -> context.getString(R.string.countdown_60_minutes)
                                120 -> context.getString(R.string.countdown_2_hours)
                                else -> "${autoCountdownMinutes}${context.getString(R.string.minutes_only, autoCountdownMinutes)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { showAutoCountdownDialog = true }
                )
            )
        )
        
        Spacer(Modifier.height(8.dp))
        
        // 其他
        SettingsCategory(
            title = context.getString(R.string.other),
            items = listOf(
                SettingsCategoryItem(
                    icon = Icons.Default.Delete,
                    title = { Text(context.getString(R.string.clear_cache)) },
                    description = {
                        Text(
                            context.getString(R.string.clear_app_cache_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        if (isCalculatingCache) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp)
                        } else {
                            Text(
                                formatBytes(cacheSize),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = { showClearCacheDialog = true }
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.FormatQuote,
                    title = { Text(context.getString(R.string.daily_quote)) },
                    description = {
                        Text(
                            context.getString(R.string.daily_quote_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = onNavigateToQuoteHistory
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.SystemUpdate,
                    title = { Text(context.getString(R.string.software_update)) },
                    description = {
                        Text(
                            context.getString(R.string.check_and_update_to_latest_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            "v$currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { showUpdateDialog = true }
                ),
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.ic_telegram),
                    title = { Text(context.getString(R.string.join_group)) },
                    description = {
                        Text(
                            context.getString(R.string.join_group_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        val telegramUrl = Constants.TELEGRAM_URL
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开链接，请检查是否安装了Telegram或浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.Info,
                    title = { Text(context.getString(R.string.about_xmsleep)) },
                    description = {
                        Text(
                            context.getString(R.string.view_app_info_version_copyright),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showAboutDialog = true }
                )
            )
        )
        }
    }
        
        // 悬浮按钮层（始终在最上层）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 天气设置按钮（仅在天气功能开启时显示）
                if (weatherEnabled) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            onClick = {
                                val lastWeather = org.xmsleep.app.weather.WeatherSoundMapper.getLastWeather(context)
                                currentWeatherCodeForDialog = lastWeather?.weatherCode ?: 0
                                showWeatherEditDialog = true
                            },
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cloud,
                                    contentDescription = "天气设置",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 灯泡按钮（添加圆角矩形背景）
                    Surface(
                        onClick = { showPullRing = !showPullRing },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.light_24px),
                                contentDescription = if (isContentHidden) "显示内容" else "隐藏内容",
                                tint = if (isContentHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // 拉环控制（从灯泡下方掉落，居中对齐）
                    if (showPullRing) {
                        org.xmsleep.app.ui.components.PullRingControl(
                            isContentHidden = isContentHidden,
                            onToggle = {
                                isContentHidden = !isContentHidden
                                // 切换后自动收起拉环
                                showPullRing = false
                            },
                            animationProgress = pullRingProgress
                        )
                    }
                }
            }
        }
        
        // 倒计时显示（左上角，仅在内容隐藏且倒计时激活时显示）
        if (isContentHidden && isTimerActive && timeLeftMillis > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "倒计时",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = formatTimeLeft(timeLeftMillis),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
        
        // 软件更新对话框
        if (showUpdateDialog) {
            // 如果状态是Installing，检查是否有已下载的文件，如果有则重置为Downloaded状态
            // 注意：resetInstallingStateIfFileExists方法已移除，如需可以重新实现
            // LaunchedEffect(showUpdateDialog, updateState) {
            //     if (showUpdateDialog && updateState is org.xmsleep.app.update.UpdateState.Installing) {
            //         delay(100) // 短暂延迟确保UpdateDialog已初始化
            //         updateViewModel.resetInstallingStateIfFileExists()
            //     }
            // }
            
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                updateViewModel = updateViewModel,
                currentLanguage = currentLanguage
            )
        }
        
        // 关于对话框
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                currentLanguage = currentLanguage,
                context = context
            )
        }
    
        // 缓存清理对话框
        if (showClearCacheDialog) {
            ClearCacheDialog(
                onDismiss = { 
                    if (!isClearingCache) {
                        showClearCacheDialog = false
                    }
                },
                onConfirm = {
                    // 立即关闭对话框，避免重复显示
                    showClearCacheDialog = false
                    isClearingCache = true
                    scope.launch {
                        try {
                            clearApplicationCache(context)
                            cacheSize = 0L  // 清理后重置缓存大小
                            Toast.makeText(context, context.getString(R.string.cache_cleared_success), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.cache_clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        } finally {
                            isClearingCache = false
                            // 清理完成后重新计算缓存大小
                            cacheSize = calculateCacheSize(context)
                        }
                    }
                },
                isClearing = isClearingCache
            )
        }
        
        // 一键调整音量对话框
        var volume by remember { 
            mutableStateOf(
                // 获取第一个正在播放的声音的音量作为默认值，优先检查本地音频，然后检查远程音频
                audioManager.getPlayingSounds().firstOrNull()?.let { 
                    audioManager.getVolume(it) 
                } ?: audioManager.getPlayingRemoteSoundIds().firstOrNull()?.let {
                    audioManager.getRemoteVolume(it)
                } ?: 0.5f
            )
        }
        
        if (showVolumeDialog) {
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text(context.getString(R.string.adjust_all_volume)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            context.getString(R.string.apply_to_all_playing_sounds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 音量滑块
                        Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = volume,
                            onValueChange = { 
                                volume = it
                                // 实时应用到所有本地声音
                                val localSounds = audioManager.getPlayingSounds()
                                localSounds.forEach { sound ->
                                    audioManager.setVolume(sound, volume)
                                }
                                // 实时应用到所有远程声音（繁星页面）
                                val remoteSoundIds = audioManager.getPlayingRemoteSoundIds()
                                Logger.d("VolumeDialog", "正在播放的远程音频数量: ${remoteSoundIds.size}, IDs: $remoteSoundIds")
                                remoteSoundIds.forEach { soundId ->
                                    Logger.d("VolumeDialog", "设置远程音频音量: $soundId = $volume")
                                    audioManager.setRemoteVolume(soundId, volume)
                                }
                                // 实时更新显示的音量数值
                                currentVolumeDisplay = volume
                            },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..1f,
                                steps = 19  // 0到100，步长5%
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
                    TextButton(onClick = { 
                        // 应用到所有本地声音（包括未播放的，以便下次播放时使用）
                        listOf(
                            org.xmsleep.app.audio.AudioManager.Sound.UMBRELLA_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.ROWING,
                            org.xmsleep.app.audio.AudioManager.Sound.OFFICE,
                            org.xmsleep.app.audio.AudioManager.Sound.LIBRARY,
                            org.xmsleep.app.audio.AudioManager.Sound.HEAVY_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.TYPEWRITER,
                            org.xmsleep.app.audio.AudioManager.Sound.THUNDER,
                            org.xmsleep.app.audio.AudioManager.Sound.CLOCK,
                            org.xmsleep.app.audio.AudioManager.Sound.FOREST_BIRDS,
                            org.xmsleep.app.audio.AudioManager.Sound.DRIFTING,
                            org.xmsleep.app.audio.AudioManager.Sound.CAMPFIRE,
                            org.xmsleep.app.audio.AudioManager.Sound.WIND,
                            org.xmsleep.app.audio.AudioManager.Sound.KEYBOARD,
                            org.xmsleep.app.audio.AudioManager.Sound.SNOW_WALKING
                        ).forEach { sound ->
                            audioManager.setVolume(sound, volume)
                        }
                        // 应用到所有正在播放的远程声音（繁星页面）
                        val remoteSoundIds = audioManager.getPlayingRemoteSoundIds()
                        remoteSoundIds.forEach { soundId ->
                            audioManager.setRemoteVolume(soundId, volume)
                        }
                        // 更新显示的音量数值
                        currentVolumeDisplay = volume
                        showVolumeDialog = false
                        Toast.makeText(context, context.getString(R.string.volume_set_to, (volume * 100).toInt()), Toast.LENGTH_SHORT).show()
                    }) {
                        Text(context.getString(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVolumeDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        // 语言选择弹窗
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { language: LanguageManager.Language ->
                    LanguageManager.setLanguage(context, language)
                    onLanguageChange(language) // 更新语言状态，触发实时切换
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
        
        // 背景选择对话框
        if (showBackgroundDialog) {
            // 保存打开对话框时的原始选择
            val originalSelection = remember { backgroundSelection }
            // 跟踪临时选择
            var tempSelection by remember { mutableStateOf(backgroundSelection) }
            
            Logger.d("SettingsScreen", "打开背景对话框，当前选择: $backgroundSelection")
            
            BackgroundSelectionDialog(
                currentSelection = tempSelection,
                onSelectionChange = { selection ->
                    // 实时预览：只更新临时选择和UI显示
                    Logger.d("SettingsScreen", "选择变化（预览）: $selection")
                    tempSelection = selection
                    onBackgroundSelectionChange(selection) // 实时预览
                },
                onDismiss = {
                    // 取消时恢复原始选择
                    Logger.d("SettingsScreen", "取消，恢复原始选择: $originalSelection")
                    onBackgroundSelectionChange(originalSelection)
                    showBackgroundDialog = false
                },
                onConfirm = {
                    // 确认时：确保状态和持久化都更新
                    Logger.d("SettingsScreen", "确认，保存选择: $tempSelection")
                    // 注意：onBackgroundSelectionChange 内部已经调用了 saveBackgroundSelection
                    // 所以这里只需要调用一次即可
                    onBackgroundSelectionChange(tempSelection)
                    showBackgroundDialog = false
                },
                currentLanguage = currentLanguage // 传递当前语言以强制重组
            )
        }
        
        // 自动倒计时选择对话框
        if (showAutoCountdownDialog) {
            val countdownOptions = listOf(
                0 to context.getString(R.string.no_countdown),
                30 to context.getString(R.string.countdown_30_minutes),
                45 to context.getString(R.string.countdown_45_minutes),
                60 to context.getString(R.string.countdown_60_minutes),
                120 to context.getString(R.string.countdown_2_hours)
            )
            
            AlertDialog(
                onDismissRequest = { showAutoCountdownDialog = false },
                title = { Text(context.getString(R.string.select_countdown_time)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        countdownOptions.forEach { (minutes, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        autoCountdownMinutes = minutes
                                        org.xmsleep.app.preferences.PreferencesManager.saveAutoCountdownMinutes(context, minutes)
                                        showAutoCountdownDialog = false
                                        Toast.makeText(
                                            context,
                                            "${context.getString(R.string.current_setting)}: $label",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = autoCountdownMinutes == minutes,
                                    onClick = {
                                        autoCountdownMinutes = minutes
                                        org.xmsleep.app.preferences.PreferencesManager.saveAutoCountdownMinutes(context, minutes)
                                        showAutoCountdownDialog = false
                                        Toast.makeText(
                                            context,
                                            "${context.getString(R.string.current_setting)}: $label",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAutoCountdownDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        // 天气编辑对话框
        if (showWeatherEditDialog) {
            val lastWeather = org.xmsleep.app.weather.WeatherSoundMapper.getLastWeather(context)
            val weatherCode = lastWeather?.weatherCode ?: currentWeatherCodeForDialog
            WeatherEditDialog(
                context = context,
                weatherCode = if (weatherCode != 0) weatherCode else 0,
                onDismiss = { showWeatherEditDialog = false },
                onRefreshWeather = {
                    // 刷新天气功能在设置页面不需要实现
                }
            )
        }
}

/**
 * 格式化剩余时间
 */
private fun formatTimeLeft(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
