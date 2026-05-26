package org.xmsleep.app.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.navigation.ProvideNavigator
import org.xmsleep.app.navigation.rememberXMSleepNavigator
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.ui.settings.SettingsScreen
import org.xmsleep.app.ui.settings.ThemeSettingsScreen
import org.xmsleep.app.ui.starsky.StarSkyScreen
import org.xmsleep.app.ui.breathing.BreathingScreen
import org.xmsleep.app.ui.flipclock.FlipClockScreen
import org.xmsleep.app.ui.tomato.TomatoTimerScreen
import org.xmsleep.app.update.UpdateDialog
import org.xmsleep.app.utils.Logger
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import org.xmsleep.app.weather.WeatherSoundMapper

/**
 * 主屏幕 - 包含底部导航和页面切换
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun MainScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    hideAnimation: Boolean,
    backgroundSelection: org.xmsleep.app.ui.BackgroundSelection,
    soundCardsColumnsCount: Int,
    currentLanguage: LanguageManager.Language,
    audioPermissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    locationPermissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    onAudioPermissionGranted: () -> Unit,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onHideAnimationChange: (Boolean) -> Unit,
    onBackgroundSelectionChange: (org.xmsleep.app.ui.BackgroundSelection) -> Unit,
    onSoundCardsColumnsCountChange: (Int) -> Unit,
    paletteColors: List<androidx.compose.ui.graphics.Color>
) {
    // 使用Navigator接口来管理导航
    val navigator = rememberXMSleepNavigator()
    var selectedItem by remember { mutableIntStateOf(1) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
    // 获取 Activity - 使用 LifecycleOwner
    // 注意：复用下面的 lifecycleOwner 变量
    var mainActivity by remember { mutableStateOf<android.app.Activity?>(null) }
    
    // lifecycleOwner 在下面声明，这里直接使用
    val lifecycleOwnerForActivity = LocalLifecycleOwner.current
    
    LaunchedEffect(lifecycleOwnerForActivity) {
        mainActivity = lifecycleOwnerForActivity as? android.app.Activity
    }
    
    // Haze状态用于毛玻璃效果
    val hazeState = remember { HazeState() }
    
    // 底部导航栏的背景色（用于毛玻璃效果）
    val navBarBackgroundColor = MaterialTheme.colorScheme.surface
    
    // 最近播放弹窗显示设置
    var showRecentPlayDialogSetting by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getShowRecentPlayDialog(context))
    }
    
    // 本地音频权限相关
    val requiredPermission = if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionGrantedPending by remember { mutableStateOf(false) }
    
    // 监听权限授予状态，导航到本地音频页面
    LaunchedEffect(permissionGrantedPending) {
        if (permissionGrantedPending) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                requiredPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            if (hasPermission) {
                navigator.navController.navigate("local_audio")
                permissionGrantedPending = false
            }
        }
    }
    
    // 监听权限变化（当用户从设置返回时）
    val permissionLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var shouldCheckPermissionOnResume by remember { mutableStateOf(false) }
    DisposableEffect(permissionLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && shouldCheckPermissionOnResume) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    requiredPermission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasPermission) {
                    // 权限已授予，进入页面
                    navigator.navController.navigate("local_audio")
                    shouldCheckPermissionOnResume = false
                }
            }
        }
        permissionLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            permissionLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 用于触发浮动按钮收缩的状态
    var shouldCollapseFloatingButton by remember { mutableStateOf(false) }
    
    // 用于强制收缩悬浮播放按钮的状态（当底部预设模块展开时）
    var forceCollapseFloatingButton by remember { mutableStateOf(false) }
    
    // 设置页面内容隐藏状态（用于隐藏底部导航栏）
    var isSettingsContentHidden by remember { mutableStateOf(false) }
    
    // 自动更新检查（全局共享）
    val updateViewModel = remember { org.xmsleep.app.update.UpdateViewModel(context) }
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
            val version = packageInfo?.versionName ?: "0.0.0"
            Logger.d("UpdateCheck", "读取到的版本号: $version")
            version
        } catch (e: Exception) {
            Logger.e("UpdateCheck", "读取版本号失败，使用默认值", e)
            "0.0.0" // 使用最低版本号作为默认值，确保能检测到更新
        }
    }
    
    // 每次进入主页时静默检查更新（不自动弹窗）
    LaunchedEffect(Unit) {
        Logger.d("UpdateCheck", "开始后台静默检查更新，当前版本: $currentVersion")
        // 静默检查更新，不弹窗，只更新状态和显示图标
        updateViewModel.startAutomaticCheckLatestVersion(currentVersion)
    }
    
    // 监听生命周期，当应用恢复时检查更新和待安装的文件
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val timerManager = remember { org.xmsleep.app.timer.TimerManager.getInstance() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Logger.d("UpdateCheck", "Activity resumed, 检查更新和待安装文件")
                
                // 先检查是否有待安装的文件且权限已授予
                if (updateViewModel.checkPendingInstall()) {
                    Logger.d("UpdateCheck", "检测到待安装文件且权限已授予")
                    // 在协程中延迟一小段时间确保UI已准备好
                    scope.launch {
                        delay(500)
                        // 自动重试安装
                        updateViewModel.autoRetryInstall()
                    }
                } else {
                    // 应用恢复时也检查更新（会受1小时间隔限制）
                    updateViewModel.startAutomaticCheckLatestVersion(currentVersion)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 当前激活的预设（1, 2, 3）
    var activePreset by remember { 
        mutableIntStateOf(org.xmsleep.app.preferences.PreferencesManager.getActivePreset(context))
    }
    
    // 天气推荐是否开启
    var weatherEnabled by remember { mutableStateOf(WeatherSoundMapper.isEnabled(context)) }
    
    // 定期检查天气开关状态
    LaunchedEffect(Unit) {
        while (true) {
            val enabled = WeatherSoundMapper.isEnabled(context)
            if (enabled != weatherEnabled) {
                weatherEnabled = enabled
            }
            delay(1000)
        }
    }
    
    // 3个预设的固定声音列表
    val preset1Sounds = remember { 
        val saved = org.xmsleep.app.preferences.PreferencesManager.getPresetLocalPinned(context, 1)
        mutableStateOf(saved.mapNotNull { name ->
            try { org.xmsleep.app.audio.AudioManager.Sound.valueOf(name) } catch (e: Exception) { null }
        }.toMutableSet())
    }
    val preset2Sounds = remember { 
        val saved = org.xmsleep.app.preferences.PreferencesManager.getPresetLocalPinned(context, 2)
        mutableStateOf(saved.mapNotNull { name ->
            try { org.xmsleep.app.audio.AudioManager.Sound.valueOf(name) } catch (e: Exception) { null }
        }.toMutableSet())
    }
    val preset3Sounds = remember { 
        val saved = org.xmsleep.app.preferences.PreferencesManager.getPresetLocalPinned(context, 3)
        mutableStateOf(saved.mapNotNull { name ->
            try { org.xmsleep.app.audio.AudioManager.Sound.valueOf(name) } catch (e: Exception) { null }
        }.toMutableSet())
    }
    
    // 获取当前激活预设的声音列表（根据 activePreset 动态切换）
    val pinnedSounds = when (activePreset) {
        1 -> preset1Sounds
        2 -> preset2Sounds
        3 -> preset3Sounds
        else -> preset1Sounds
    }
    
    // 保存当前预设的固定声音到 SharedPreferences
    LaunchedEffect(preset1Sounds.value) {
        org.xmsleep.app.preferences.PreferencesManager.savePresetLocalPinned(
            context, 1, preset1Sounds.value.map { it.name }.toSet()
        )
    }
    LaunchedEffect(preset2Sounds.value) {
        org.xmsleep.app.preferences.PreferencesManager.savePresetLocalPinned(
            context, 2, preset2Sounds.value.map { it.name }.toSet()
        )
    }
    LaunchedEffect(preset3Sounds.value) {
        org.xmsleep.app.preferences.PreferencesManager.savePresetLocalPinned(
            context, 3, preset3Sounds.value.map { it.name }.toSet()
        )
    }
    
    // 保存当前激活的预设
    LaunchedEffect(activePreset) {
        org.xmsleep.app.preferences.PreferencesManager.saveActivePreset(context, activePreset)
    }
    
    // AudioManager实例（用于播放/暂停快捷播放的声音）
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    
    // 监听播放状态，自动启动/停止MusicService
    val hasPlayingSounds = remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // 实时监听播放状态
        while (true) {
            val isPlaying = audioManager.hasAnyPlayingSounds()
            if (isPlaying != hasPlayingSounds.value) {
                hasPlayingSounds.value = isPlaying
                
                if (isPlaying) {
                    // 有音频播放，启动服务
                    Logger.d("MainScreen", "检测到音频播放，启动MusicService")
                    audioManager.startMusicService(context)
                } else {
                    // 所有音频已停止
                    Logger.d("MainScreen", "所有音频已停止")
                }
            }
            delay(1000) // 每秒检查一次
        }
    }
    
    // 应用退出时清理服务
    DisposableEffect(Unit) {
        onDispose {
            Logger.d("MainScreen", "MainScreen onDispose")
            // 如果没有播放中的声音，停止服务
            if (!audioManager.hasAnyPlayingSounds()) {
                audioManager.stopMusicService(context)
            }
        }
    }
    
    // PreferencesManager实例（用于管理预设的远程声音）
    val preferencesManager = remember { org.xmsleep.app.preferences.PreferencesManager }
    
    // 检查所有预设是否都为空（只有当所有3个预设都为空时才隐藏预设模块）
    // 修复：同时检查本地音频预设和远程音频固定状态
    var preset1RemotePinned by remember { mutableStateOf(preferencesManager.getPresetRemotePinned(context, 1)) }
    var preset2RemotePinned by remember { mutableStateOf(preferencesManager.getPresetRemotePinned(context, 2)) }
    var preset3RemotePinned by remember { mutableStateOf(preferencesManager.getPresetRemotePinned(context, 3)) }
    val allRemotePinned = preset1RemotePinned + preset2RemotePinned + preset3RemotePinned
    
    val defaultAreaHasSounds = preset1Sounds.value.isNotEmpty() || 
                                preset2Sounds.value.isNotEmpty() || 
                                preset3Sounds.value.isNotEmpty() ||
                                allRemotePinned.isNotEmpty()
    
    // 添加调试日志跟踪预设模块显示状态
    LaunchedEffect(defaultAreaHasSounds) {
        Logger.d("MainScreen", "预设模块显示状态变化: $defaultAreaHasSounds, 本地预设1=${preset1Sounds.value.size}, 预设2=${preset2Sounds.value.size}, 预设3=${preset3Sounds.value.size}, 远程固定=${allRemotePinned.size}")
    }
    
    // 实时监听所有预设的远程音频固定状态变化
    LaunchedEffect(Unit) {
        while (true) {
            delay(200) // 提高检查频率到200ms
            val newPreset1RemotePinned = preferencesManager.getPresetRemotePinned(context, 1)
            val newPreset2RemotePinned = preferencesManager.getPresetRemotePinned(context, 2)
            val newPreset3RemotePinned = preferencesManager.getPresetRemotePinned(context, 3)
            val newAllRemotePinned = newPreset1RemotePinned + newPreset2RemotePinned + newPreset3RemotePinned
            
            if (newAllRemotePinned != allRemotePinned) {
                Logger.d("MainScreen", "所有预设远程音频固定状态变化: ${allRemotePinned.size} -> ${newAllRemotePinned.size}")
                preset1RemotePinned = newPreset1RemotePinned
                preset2RemotePinned = newPreset2RemotePinned
                preset3RemotePinned = newPreset3RemotePinned
            }
        }
    }
    
    // 实时检测快捷播放的播放状态（使用LaunchedEffect定期更新）
    var defaultAreaSoundsPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(preset1Sounds.value, preset2Sounds.value, preset3Sounds.value, activePreset, Unit) {
        while (true) {
            val currentPresetSounds = when (activePreset) {
                1 -> preset1Sounds.value
                2 -> preset2Sounds.value
                3 -> preset3Sounds.value
                else -> preset1Sounds.value
            }
            defaultAreaSoundsPlaying = currentPresetSounds.any { audioManager.isPlayingSound(it) }
            delay(300) // 每300ms检查一次
        }
    }
    
    // 监听当前路由，判断是否在二级页面
    val currentBackStackEntry by navigator.navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isInSecondaryPage = currentRoute in listOf("theme", "local_audio", "quoteHistory", "flipclock", "tomato_timer")
    val isMainRoute = !isInSecondaryPage  // 主页面 = 不在二级页面
    val isFlipClockPage = currentRoute == "flipclock"
    
    // 翻页时钟页面隐藏系统栏（延迟2秒执行）
    var systemBarHidden by remember { mutableStateOf(false) }
    LaunchedEffect(isFlipClockPage) {
        if (isFlipClockPage) {
            delay(2000) // 延迟2秒隐藏系统栏
            systemBarHidden = true
            mainActivity?.window?.let { window ->
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                windowInsetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            systemBarHidden = false
            mainActivity?.window?.let { window ->
                val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController?.show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    // 使用ProvideNavigator提供导航器给子组件
    ProvideNavigator(navigator) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 最底层：动画背景（如果用户选择了背景）
            if (!hideAnimation) {
                org.xmsleep.app.ui.components.AnimatedBackground(
                    backgroundSelection = backgroundSelection,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 内容层 - 应用haze捕获内容用于模糊
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {},
                    floatingActionButton = {},
                    bottomBar = {} // 底部导航栏移到外面作为独立层
                ) { paddingValues ->
                    // NavHost 始终存在，用于处理二级页面导航
                    NavHost(
                        navController = navigator.navController,
                        startDestination = "main",
                        modifier = Modifier.fillMaxSize()
                    ) {
            // 主页面路由（显示 AnimatedContent）
            composable("main") {
                // 主页面：直接根据 selectedItem 切换内容（支持左右滑动切换）
                AnimatedContent(
                    targetState = selectedItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // 手势结束时判断是否切换页面
                                    val threshold = size.width * 0.25f
                                    when {
                                        // 向右滑动
                                        accumulatedDrag > threshold -> {
                                            when (selectedItem) {
                                                2 -> selectedItem = 1  // 从繁星到白噪音
                                                4 -> selectedItem = 2  // 从呼吸到繁星
                                                3 -> selectedItem = 4  // 从设置到呼吸
                                            }
                                        }
                                        // 向左滑动
                                        accumulatedDrag < -threshold -> {
                                            when (selectedItem) {
                                                1 -> selectedItem = 2  // 从白噪音到繁星
                                                2 -> selectedItem = 4  // 从繁星到呼吸
                                                4 -> selectedItem = 3  // 从呼吸到设置
                                            }
                                        }
                                    }
                                    accumulatedDrag = 0f
                                }
                            ) { change, dragAmount ->
                                accumulatedDrag += dragAmount
                            }
                        },
                    transitionSpec = {
                        // 改进的过渡动画：使用滑动和淡入淡出效果
                        // 切换到设置页 (tab 3) 时从右往左，其他情况保持原有逻辑
                        val direction = if (targetState == 3 && initialState != 3) {
                            1 // 切换到设置页时，新页面从右边进入
                        } else if (initialState == 3 && targetState != 3) {
                            -1 // 从设置页切出时，旧页面向右退出
                        } else {
                            if (targetState > initialState) 1 else -1
                        }
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth * direction },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth * direction },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    },
                    label = "tab_switch"
                ) { currentTab ->
                    // tab 切换时，触发浮动按钮收缩
                    LaunchedEffect(currentTab) {
                        shouldCollapseFloatingButton = true
                        delay(100) // 短暂延迟后重置
                        shouldCollapseFloatingButton = false
                    }
                    when (currentTab) {
                        1 -> {
                            // 白噪音页面
                            org.xmsleep.app.ui.SoundsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                hideAnimation = hideAnimation,
                                backgroundSelection = backgroundSelection,
                                onBackgroundSelectionChange = onBackgroundSelectionChange,
                                columnsCount = soundCardsColumnsCount,
                                onColumnsCountChange = onSoundCardsColumnsCountChange,
                                preset1Sounds = preset1Sounds,
                                preset2Sounds = preset2Sounds,
                                preset3Sounds = preset3Sounds,
                                activePreset = activePreset,
                                onActivePresetChange = { newPreset -> activePreset = newPreset },
                                hasAnyPresetItems = defaultAreaHasSounds,
                                onNavigateToFlipClock = {
                                    navigator.navigateToTomatoTimer()
                                },
                                onScrollDetected = {
                                    // 滚动时，触发浮动按钮收缩
                                    shouldCollapseFloatingButton = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(100) // 短暂延迟后重置
                                        shouldCollapseFloatingButton = false
                                    }
                                },
                                onQuickPlayExpand = {
                                    // 当快捷播放展开时，强制收缩悬浮播放按钮
                                    forceCollapseFloatingButton = true
                                    // 短暂延迟后重置强制收缩状态
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(100)
                                        forceCollapseFloatingButton = false
                                    }
                                },
                                updateViewModel = updateViewModel
                            )
                        }
                        2 -> {
                            // 星空页面
                            StarSkyScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                activePreset = activePreset,
                                onScrollDetected = {
                                    // 滚动时，触发浮动按钮收缩
                                    shouldCollapseFloatingButton = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(100) // 短暂延迟后重置
                                        shouldCollapseFloatingButton = false
                                    }
                                },
                                onNavigateToLocalAudio = {
                                    // 检查权限
                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        requiredPermission
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasPermission) {
                                        // 有权限，直接进入页面
                                        navigator.navController.navigate("local_audio")
                                    } else {
                                        // 没有权限，显示对话框
                                        showPermissionDialog = true
                                    }
                                }
                            )
                        }
                        4 -> {
                            // 呼吸页面
                            BreathingScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                activity = mainActivity
                            )
                        }
                        3 -> {
                            SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                hideAnimation = hideAnimation,
                                onHideAnimationChange = onHideAnimationChange,
                                backgroundSelection = backgroundSelection,
                                onBackgroundSelectionChange = onBackgroundSelectionChange,
                                paletteColors = paletteColors,
                                currentColor = selectedColor,
                                onColorChange = onColorChange,
                                updateViewModel = updateViewModel,
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChange,
                                onScrollDetected = {
                                    // 滚动时，触发浮动按钮收缩
                                    shouldCollapseFloatingButton = true
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(100) // 短暂延迟后重置
                                        shouldCollapseFloatingButton = false
                                    }
                                },
                                onNavigateToTheme = { 
                                    navigator.navigateToTheme()
                                },
                                onNavigateToSounds = {
                                    // 不再需要导航到声音页面，因为已经是独立tab
                                },
                                onNavigateToQuoteHistory = {
                                    navigator.navigateToQuoteHistory()
                                },
                                onNavigateToFlipClock = {
                                    navigator.navigateToFlipClock()
                                },
                                pinnedSounds = pinnedSounds,
                                locationPermissionLauncher = locationPermissionLauncher,
                                onContentHiddenChange = { isHidden ->
                                    isSettingsContentHidden = isHidden
                                }
                            )
                        }
                        else -> { /* 不应该到达这里 */ }
                    }
                }
            }
            
            // 二级页面路由
            composable("theme") {
                ThemeSettingsScreen(
                    darkMode = darkMode,
                    selectedColor = selectedColor,
                    useDynamicColor = useDynamicColor,
                    useBlackBackground = useBlackBackground,
                    onDarkModeChange = onDarkModeChange,
                    onColorChange = onColorChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onBlackBackgroundChange = onBlackBackgroundChange,
                    onBack = { navigator.popBackStack() },
                    onScrollDetected = {
                        // 滚动时收缩悬浮按钮
                        shouldCollapseFloatingButton = true
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            shouldCollapseFloatingButton = false
                        }
                    }
                )
            }
            
            composable("local_audio") {
                org.xmsleep.app.ui.LocalAudioScreen(
                    modifier = Modifier.fillMaxSize(),
                    onBack = { navigator.popBackStack() }
                )
            }
            
            composable("quoteHistory") {
                org.xmsleep.app.quote.QuoteHistoryScreen(
                    onBack = { navigator.popBackStack() },
                    onScrollDetected = {
                        // 滚动时收缩悬浮按钮
                        shouldCollapseFloatingButton = true
                    }
                )
            }
            
            composable("flipclock") {
                FlipClockScreen(
                    onBack = { navigator.popBackStack() }
                )
            }
            
            composable("tomato_timer") {
                TomatoTimerScreen(
                    onBack = { navigator.popBackStack() }
                )
            }
        }
                }
            }
            
            // 底部导航栏 - 作为独立层，应用毛玻璃效果
            // 只在主页面显示，且设置页面内容未隐藏时显示
            androidx.compose.animation.AnimatedVisibility(
                visible = isMainRoute && !isSettingsContentHidden,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = androidx.compose.animation.fadeIn(animationSpec = tween(0)),
                exit = androidx.compose.animation.fadeOut(animationSpec = tween(0))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .hazeChild(
                            state = hazeState
                        ) {
                            blurRadius = 10.dp
                            noiseFactor = 0.16f
                            backgroundColor = navBarBackgroundColor
                        },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.extraLarge,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 白噪音 Tab
                        NavigationBarItem(
                            selected = selectedItem == 1,
                            onClick = { selectedItem = 1 },
                            icon = Icons.Default.LocalFlorist,
                            label = context.getString(R.string.tab_white_noise) // 显示文字
                        )
                        
                        // 繁星 Tab
                        NavigationBarItem(
                            selected = selectedItem == 2,
                            onClick = { selectedItem = 2 },
                            icon = Icons.Default.Satellite,
                            label = context.getString(R.string.tab_starsky) // 显示文字
                        )
                        
                        // 呼吸 Tab
                        NavigationBarItem(
                            selected = selectedItem == 4,
                            onClick = { selectedItem = 4 },
                            icon = Icons.Default.SelfImprovement,
                            label = context.getString(R.string.tab_breathing) // 显示文字
                        )
                        
                        // 设置 Tab
                        NavigationBarItem(
                            selected = selectedItem == 3,
                            onClick = { selectedItem = 3 },
                            icon = Icons.Default.Settings,
                            label = context.getString(R.string.tab_settings) // 显示文字
                        )
                    }
                }
            }
        }

        // 全局浮动播放按钮（新版本 - 吸附式交互）
        org.xmsleep.app.ui.FloatingPlayButtonNew(
            audioManager = audioManager,
            selectedTab = selectedItem, // 传递当前选中的 tab
            shouldCollapse = shouldCollapseFloatingButton, // 传递收缩标志
            activePreset = activePreset,
            forceCollapse = forceCollapseFloatingButton, // 传递强制收缩标志
            onExpandStateChange = { isExpanded ->
                // 当悬浮按钮展开时，收缩底部预设弹窗
                if (isExpanded) {
                    // 通过修改 PreferencesManager 中的快捷播放展开状态来收缩预设弹窗
                    org.xmsleep.app.preferences.PreferencesManager.saveQuickPlayExpanded(context, false)
                }
            },
            onAddToPreset = { localSounds, remoteSoundIds ->
                // 获取当前预设的 MutableState
                val currentPresetSounds = when (activePreset) {
                    1 -> preset1Sounds
                    2 -> preset2Sounds
                    3 -> preset3Sounds
                    else -> preset1Sounds
                }
                
                // 获取当前预设已有的本地和远程声音数量
                val currentLocalSize = currentPresetSounds.value.size
                // 修复：使用预设特定的远程音频置顶存储
                val currentRemotePinned = preferencesManager.getPresetRemotePinned(context, activePreset)
                val currentRemoteSize = currentRemotePinned.size
                val currentTotalSize = currentLocalSize + currentRemoteSize
                val maxSize = 10
                
                // 计算可以添加多少个
                val canAddCount = (maxSize - currentTotalSize).coerceAtLeast(0)
                
                if (canAddCount == 0) {
                    // 预设已满
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.preset_full, activePreset),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // 合并本地和远程声音，按顺序添加（最多添加 canAddCount 个）
                    val totalSoundsToAdd = localSounds.size + remoteSoundIds.size
                    val actualAddCount = minOf(canAddCount, totalSoundsToAdd)
                    
                    var remainingSlots = actualAddCount
                    var addedCount = 0
                    
                    // 先添加本地声音
                    if (remainingSlots > 0 && localSounds.isNotEmpty()) {
                        val localToAdd = localSounds.take(remainingSlots)
                        val newLocalSet = currentPresetSounds.value.toMutableSet()
                        newLocalSet.addAll(localToAdd)
                        currentPresetSounds.value = newLocalSet
                        addedCount += localToAdd.size
                        remainingSlots -= localToAdd.size
                    }
                    
                    // 再添加远程声音
                    if (remainingSlots > 0 && remoteSoundIds.isNotEmpty()) {
                        val remoteToAdd = remoteSoundIds.take(remainingSlots)
                        // 修复：使用预设特定的远程音频置顶存储
                        val newRemoteSet = currentRemotePinned.toMutableSet()
                        newRemoteSet.addAll(remoteToAdd)
                        preferencesManager.savePresetRemotePinned(context, activePreset, newRemoteSet)
                        addedCount += remoteToAdd.size
                    }
                    
                    // 显示成功提示
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.added_to_preset, addedCount, activePreset),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        
        // 最近播放弹窗 - 只在应用启动时显示一次（根据用户设置）
        var showRecentPlayDialog by remember { mutableStateOf(false) }
        
        // 使用静态变量确保只在应用启动时检查一次
        LaunchedEffect(Unit) {
            if (!hasCheckedRecentPlayOnLaunch) {
                hasCheckedRecentPlayOnLaunch = true

                // 1. 检查是否开启了自动播放功能
                val autoPlayEnabled = org.xmsleep.app.preferences.PreferencesManager.getAutoPlayOnStart(context)
                if (autoPlayEnabled) {
                    // 获取当前激活预设的声音列表
                    val soundsToPlay = when (activePreset) {
                        1 -> preset1Sounds.value
                        2 -> preset2Sounds.value
                        3 -> preset3Sounds.value
                        else -> preset1Sounds.value
                    }

                    // 检查远程音频
                    val remotePinned = when (activePreset) {
                        1 -> org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 1)
                        2 -> org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 2)
                        3 -> org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 3)
                        else -> emptySet()
                    }

                    if (soundsToPlay.isNotEmpty() || remotePinned.isNotEmpty()) {
                        val audioManager = org.xmsleep.app.audio.AudioManager.getInstance()

                        // 播放本地预设声音
                        soundsToPlay.forEach { sound ->
                            audioManager.playSound(context, sound)
                        }

                        // 播放远程预设声音（繁星页面音频）
                        if (remotePinned.isNotEmpty()) {
                            val resourceManager = org.xmsleep.app.audio.AudioResourceManager.getInstance(context)
                            val cachedManifest = resourceManager.getCachedManifest()
                            cachedManifest?.sounds?.filter { it.id in remotePinned }?.forEach { metadata ->
                                scope.launch {
                                    val uri = resourceManager.getSoundUri(metadata)
                                    if (uri != null) {
                                        audioManager.playRemoteSound(context, metadata, uri)
                                    }
                                }
                            }
                        }

                        // 检查是否设置了自动倒计时
                        val autoCountdownMinutes = org.xmsleep.app.preferences.PreferencesManager.getAutoCountdownMinutes(context)
                        if (autoCountdownMinutes > 0) {
                            scope.launch {
                                delay(500)
                                timerManager.startTimer(autoCountdownMinutes)
                            }
                        }

                        Logger.d("MainScreen", "自动播放预设 $activePreset 音频: 本地 ${soundsToPlay.size}, 远程 ${remotePinned.size}")
                    }
                } else {
                    // 2. 如果没有开启自动播放，检查是否开启了最近播放弹窗
                    val shouldShow = org.xmsleep.app.preferences.PreferencesManager.getShowRecentPlayDialog(context)
                    if (shouldShow) {
                        val audioManager = org.xmsleep.app.audio.AudioManager.getInstance()
                        if (audioManager.hasRecentSounds(context)) {
                            delay(500)
                            showRecentPlayDialog = true
                        }
                    }
                }
            }
        }
        
        if (showRecentPlayDialog) {
            org.xmsleep.app.ui.components.RecentPlayDialog(
                onDismiss = {
                    showRecentPlayDialog = false
                },
                onPlayRecent = {
                    val audioManager = org.xmsleep.app.audio.AudioManager.getInstance()
                    audioManager.playRecentSounds(context)
                    showRecentPlayDialog = false
                }
            )
        }
        
        // 本地音频权限请求对话框
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                title = { Text(context.getString(R.string.storage_permission_required)) },
                text = { Text(context.getString(R.string.permission_denied_hint)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            Logger.d("MainScreen", "点击授予权限按钮")
                            showPermissionDialog = false
                            // 使用从 MainActivity 传递的 audioPermissionLauncher 请求权限
                            audioPermissionLauncher.launch(requiredPermission)
                            permissionGrantedPending = true
                            onAudioPermissionGranted()
                            Logger.d("MainScreen", "权限请求已发送: $requiredPermission")
                        }
                    ) {
                        Text(context.getString(R.string.request_permission))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
    }
}

/**
 * 自定义导航栏项目组件
 */
@Composable
private fun NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?, // 改为可空类型
    modifier: Modifier = Modifier
) {
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = androidx.compose.animation.core.spring(
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy
        ),
        label = "nav_item_scale"
    )
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )
        
        // 只在 label 不为空时显示文字
        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor
            )
        }
    }
}

private var hasCheckedRecentPlayOnLaunch = false
