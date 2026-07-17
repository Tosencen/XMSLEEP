package org.xmsleep.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
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
import org.xmsleep.app.ui.components.BackgroundSettings
import org.xmsleep.app.ui.components.BackgroundSettingsSheet
import org.xmsleep.app.ui.components.LanguageSelectionDialog
import org.xmsleep.app.ui.BackgroundSelection
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.update.UpdateDialog
import org.xmsleep.app.ui.starsky.WeatherEditDialog
import org.xmsleep.app.utils.Logger

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
    paletteColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    currentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {},
    backgroundOpacity: Float = 0.2f,
    backgroundBlurRadius: Float = 0f,
    onBackgroundOpacityChange: (Float) -> Unit = {},
    onBackgroundBlurRadiusChange: (Float) -> Unit = {},
    updateViewModel: org.xmsleep.app.update.UpdateViewModel,
    currentLanguage: LanguageManager.Language,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToQuoteHistory: () -> Unit = {},
    onNavigateToFlipClock: () -> Unit = {},
    onNavigateToDiary: () -> Unit = {},
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    locationPermissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    onScrollDetected: () -> Unit = {},
    onContentHiddenChange: (Boolean) -> Unit = {},
    onShowDeveloperLetter: () -> Unit = {},
    customBackgroundUri: String? = null,
    onPickCustomBackground: () -> Unit = {},
    customBackgroundThumbnail: String? = null,
    pendingCustomBgUri: String? = null,
    pendingCustomBgThumbnail: String? = null,
    pendingCustomBgColor: androidx.compose.ui.graphics.Color? = null,
    onCommitCustomBg: () -> Unit = {},
    onCancelCustomBg: () -> Unit = {},
    onCustomColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    val timerManager = remember { org.xmsleep.app.timer.TimerManager.getInstance() }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showFeatureDialog by remember { mutableStateOf(false) }
    var showCommunityDialog by remember { mutableStateOf(false) }
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
                    onClick = { showBackgroundDialog = true }
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.AutoAwesome,
                    title = { Text(context.getString(R.string.feature_settings)) },
                    description = {
                        Text(
                            context.getString(R.string.feature_settings_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { showFeatureDialog = true }
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
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.AccessTime,
                    title = { Text(context.getString(R.string.flip_clock)) },
                    description = {
                        Text(
                            context.getString(R.string.flip_clock_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { onNavigateToFlipClock() }
                )
            )
        )

        Spacer(Modifier.height(8.dp))
        
        // 其他
        SettingsCategory(
            title = context.getString(R.string.other),
            items = listOf(
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
                    onClick = onNavigateToQuoteHistory
                ),
                SettingsCategoryItem(
                    icon = Icons.Default.GraphicEq,
                    title = { Text(context.getString(R.string.diary)) },
                    description = {
                        Text(
                            context.getString(R.string.diary_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { onNavigateToDiary() }
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
                    onClick = { showCommunityDialog = true }
                )
            )
        )

        // 底部版本信息和关于
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v$currentVersion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showUpdateDialog = true }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = context.getString(R.string.about_xmsleep),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { showAboutDialog = true }
            )
        }
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
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                updateViewModel = updateViewModel,
                currentLanguage = currentLanguage
            )
        }

        // 特色功能弹窗
        if (showFeatureDialog) {
            var showRecentPlayDialog by remember {
                mutableStateOf(PreferencesManager.getShowRecentPlayDialog(context))
            }
            var autoPlayEnabled by remember {
                mutableStateOf(PreferencesManager.getAutoPlayOnStart(context))
            }

            val hasPresetContent = remember {
                (1..3).any { i ->
                    PreferencesManager.getPresetLocalPinned(context, i).isNotEmpty() ||
                    PreferencesManager.getPresetRemotePinned(context, i).isNotEmpty()
                }
            }

            var showAutoPlayTip by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showFeatureDialog = false },
                title = {
                    Text(
                        text = context.getString(R.string.feature_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column {
                        // 启动时显示最近播放
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.show_recent_play_dialog),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = context.getString(R.string.show_recent_play_dialog_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showRecentPlayDialog,
                                onCheckedChange = { enabled ->
                                    showRecentPlayDialog = enabled
                                    PreferencesManager.saveShowRecentPlayDialog(context, enabled)
                                    if (enabled && autoPlayEnabled) {
                                        autoPlayEnabled = false
                                        PreferencesManager.setAutoPlayOnStart(context, false)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // HorizontalDivider()  // 注释掉横线
                        Spacer(modifier = Modifier.height(16.dp))

                        // 开启应用自动播放
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.auto_play_on_start),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = context.getString(R.string.auto_play_on_start_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!hasPresetContent) {
                                    IconButton(onClick = { showAutoPlayTip = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = context.getString(R.string.tip),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Switch(
                                    checked = autoPlayEnabled,
                                    enabled = hasPresetContent,
                                    onCheckedChange = { enabled ->
                                        autoPlayEnabled = enabled
                                        PreferencesManager.setAutoPlayOnStart(context, enabled)
                                        if (enabled && showRecentPlayDialog) {
                                            showRecentPlayDialog = false
                                            PreferencesManager.saveShowRecentPlayDialog(context, false)
                                        }
                                    }
                                )
                            }
                        }

                        if (showAutoPlayTip) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = context.getString(R.string.auto_play_need_preset_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // HorizontalDivider()  // 注释掉横线
                        Spacer(modifier = Modifier.height(16.dp))

                        var showRadioTab by remember {
                            mutableStateOf(PreferencesManager.getShowRadioTab(context))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.show_radio_tab),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = context.getString(R.string.show_radio_tab_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showRadioTab,
                                onCheckedChange = { enabled ->
                                    showRadioTab = enabled
                                    PreferencesManager.setShowRadioTab(context, enabled)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var showBreathingTab by remember {
                            mutableStateOf(PreferencesManager.getShowBreathingTab(context))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.show_breathing_tab),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = context.getString(R.string.show_breathing_tab_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showBreathingTab,
                                onCheckedChange = { enabled ->
                                    showBreathingTab = enabled
                                    PreferencesManager.setShowBreathingTab(context, enabled)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFeatureDialog = false }) {
                        Text(context.getString(R.string.ok))
                    }
                }
            )
        }

        // 社区对话框
        if (showCommunityDialog) {
            AlertDialog(
                onDismissRequest = { showCommunityDialog = false },
                title = {
                    Text(
                        text = context.getString(R.string.community_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // QQ 按钮
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCommunityDialog = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&card_type=group&uin=805606877"))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.unable_to_open_link), Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_qq),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(R.string.join_qq),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = context.getString(R.string.join_qq_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Telegram 按钮
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCommunityDialog = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.TELEGRAM_URL))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.unable_to_open_link_check_telegram), Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_telegram),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(R.string.join_telegram),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = context.getString(R.string.join_telegram_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // GitHub 按钮
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCommunityDialog = false
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB_URL))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.unable_to_open_link), Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_github),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = context.getString(R.string.open_github),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = context.getString(R.string.open_github_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCommunityDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }

        // 关于对话框
        if (showAboutDialog) {
                AboutDialog(
                    onDismiss = { showAboutDialog = false },
                    currentLanguage = currentLanguage,
                    context = context,
                    onShowDeveloperLetter = onShowDeveloperLetter
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
        
        // 背景选择底部弹窗
        BackgroundSettingsSheet(
            show = showBackgroundDialog,
            currentSelection = backgroundSelection,
            paletteColors = paletteColors,
            currentColor = currentColor,
            currentOpacity = backgroundOpacity,
            currentBlurRadius = backgroundBlurRadius,
            customBackgroundUri = customBackgroundUri,
            customBackgroundThumbnail = customBackgroundThumbnail,
            pendingCustomBgUri = pendingCustomBgUri,
            pendingCustomBgThumbnail = pendingCustomBgThumbnail ?: customBackgroundThumbnail,
            pendingCustomBgColor = pendingCustomBgColor,
            hasPendingCustomBg = pendingCustomBgUri != null,
            onSelectionChange = { selection ->
                Logger.d("SettingsScreen", "选择变化（预览）: $selection")
                // 只更新内存状态用于预览，不保存到 SharedPreferences
                // 只有点击确认时才保存
            },
            onColorChange = {
                onColorChange(it)
            },
            onOpacityChange = onBackgroundOpacityChange,
            onBlurRadiusChange = onBackgroundBlurRadiusChange,
            onCustomBackgroundClick = { onPickCustomBackground() },
            onDismiss = {
                Logger.d("SettingsScreen", "取消背景设置")
                onCancelCustomBg()
                showBackgroundDialog = false
            },
            onConfirm = { settings ->
                Logger.d("SettingsScreen", "确认: selection=${settings.selection}, color=${settings.color}, pendingUri=$pendingCustomBgUri")
                // Apply all settings atomically
                onBackgroundSelectionChange(settings.selection)
                onColorChange(settings.color)
                onBackgroundOpacityChange(settings.opacity)
                onBackgroundBlurRadiusChange(settings.blurRadius)
                // Commit pending custom background if applicable
                if (settings.selection == BackgroundSelection.Custom && pendingCustomBgUri != null) {
                    onCommitCustomBg()
                }
                showBackgroundDialog = false
            },
            currentLanguage = currentLanguage
        )
        
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

internal fun copyToPrivateStorage(context: Context, uri: Uri): Pair<Uri?, Uri?> {
    return try {
        val mimeType = context.contentResolver.getType(uri) ?: "image/png"
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("mp4") -> "mp4"
            else -> "png"
        }
        val bgDir = java.io.File(context.filesDir, "custom_bg")
        bgDir.mkdirs()
        val fileName = "custom_bg_${System.currentTimeMillis()}.$ext"
        val destFile = java.io.File(bgDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        val fileUri = android.net.Uri.fromFile(destFile)

        val thumbUri = if (mimeType.contains("mp4")) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val bitmap = retriever.frameAtTime
                retriever.release()
                if (bitmap != null) {
                    val thumbFile = java.io.File(bgDir, "${fileName}_thumb.jpg")
                    java.io.FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    bitmap.recycle()
                    android.net.Uri.fromFile(thumbFile)
                } else null
            } catch (_: Exception) { null }
        } else null

        Pair(fileUri, thumbUri)
    } catch (e: Exception) {
        Logger.e("SettingsScreen", "复制自定义背景文件失败", e)
        Pair(null, null)
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
