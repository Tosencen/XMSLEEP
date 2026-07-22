package org.xmsleep.app.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Favorite // explicit for icon resolution
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.navigation.ProvideNavigator
import org.xmsleep.app.navigation.rememberXMSleepNavigator
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.ui.settings.SettingsScreen
import org.xmsleep.app.ui.settings.ThemeSettingsScreen
import org.xmsleep.app.ui.starsky.StarSkyScreen
import org.xmsleep.app.ui.breathing.BreathingListScreen
import org.xmsleep.app.ui.breathing.BreathingDetailScreen
import org.xmsleep.app.ui.meditation.MeditationPlayerScreen
import org.xmsleep.app.ui.flipclock.FlipClockScreen
import org.xmsleep.app.ui.tomato.TomatoTimerScreen
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
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.viewmodel.MainViewModel
import org.xmsleep.app.ui.viewmodel.SoundsViewModel
import androidx.hilt.navigation.compose.hiltViewModel

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
    backgroundOpacity: Float = 0.2f,
    backgroundBlurRadius: Float = 0f,
    onBackgroundOpacityChange: (Float) -> Unit = {},
    onBackgroundBlurRadiusChange: (Float) -> Unit = {},
    paletteColors: List<androidx.compose.ui.graphics.Color>,
    mainViewModel: MainViewModel = hiltViewModel(),
    soundsViewModel: SoundsViewModel = hiltViewModel(),
    customBackgroundUri: String? = null,
    customBackgroundColor: Color? = null,
    customBackgroundThumbnail: String? = null,
    onPickCustomBackground: () -> Unit = {},
    pendingCustomBgUri: String? = null,
    pendingCustomBgThumbnail: String? = null,
    pendingCustomBgColor: Color? = null,
    onCommitCustomBg: () -> Unit = {},
    onCancelCustomBg: () -> Unit = {},
    onCustomColorChange: (Color) -> Unit = {}
) {
    // 使用Navigator接口来管理导航
    val navigator = rememberXMSleepNavigator()
    var selectedItem by remember { mutableIntStateOf(1) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var simpleMode by remember { mutableStateOf(prefs.getBoolean("simple_mode", false)) }
    LaunchedEffect(simpleMode) { prefs.edit().putBoolean("simple_mode", simpleMode).apply() }
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
    
    var showRadioTab by remember {
        val prefs = context.getSharedPreferences(
            org.xmsleep.app.Constants.PrefsKeys.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        mutableStateOf(prefs.getBoolean(org.xmsleep.app.Constants.PrefsKeys.SHOW_RADIO_TAB, true))
    }
    // 监听电台Tab显隐偏好变化
    val radioTabPrefListener = remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == org.xmsleep.app.Constants.PrefsKeys.SHOW_RADIO_TAB) {
                showRadioTab = prefs.getBoolean(key, true)
            }
        }
    }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences(
            org.xmsleep.app.Constants.PrefsKeys.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        prefs.registerOnSharedPreferenceChangeListener(radioTabPrefListener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(radioTabPrefListener) }
    }
    
    // 当电台 Tab 隐藏时，如果当前在电台页面则切换到默认 Tab
    LaunchedEffect(showRadioTab) {
        if (!showRadioTab && selectedItem == 5) {
            selectedItem = 1
        }
    }

    var showBreathingTab by remember {
        val prefs = context.getSharedPreferences(
            org.xmsleep.app.Constants.PrefsKeys.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        mutableStateOf(prefs.getBoolean(org.xmsleep.app.Constants.PrefsKeys.SHOW_BREATHING_TAB, true))
    }
    val breathingTabPrefListener = remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == org.xmsleep.app.Constants.PrefsKeys.SHOW_BREATHING_TAB) {
                showBreathingTab = prefs.getBoolean(key, true)
            }
        }
    }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences(
            org.xmsleep.app.Constants.PrefsKeys.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        prefs.registerOnSharedPreferenceChangeListener(breathingTabPrefListener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(breathingTabPrefListener) }
    }

    // 当呼吸 Tab 隐藏时，如果当前在呼吸页面则切换到默认 Tab
    LaunchedEffect(showBreathingTab) {
        if (!showBreathingTab && selectedItem == 4) {
            selectedItem = 1
        }
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

    // 开发者来信弹窗
    var showDeveloperLetter by remember { mutableStateOf(false) }
    
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
    
    // 动态预设管理
    var presetList by remember { mutableStateOf(PreferencesManager.getPresetList(context)) }
    var activePreset by remember {
        mutableIntStateOf(PreferencesManager.getActivePreset(context))
    }
    var presetSoundsMap by remember { mutableStateOf<Map<Int, MutableSet<AudioManager.Sound>>>(emptyMap()) }
    val pinnedSounds: MutableState<MutableSet<AudioManager.Sound>> = remember { mutableStateOf(mutableSetOf()) }

    // 初始化预设声音
    LaunchedEffect(presetList) {
        val map = mutableMapOf<Int, MutableSet<AudioManager.Sound>>()
        presetList.forEach { entry: PreferencesManager.PresetEntry ->
            val saved = PreferencesManager.getPresetLocalPinned(context, entry.id)
            map[entry.id] = saved.mapNotNull { name ->
                try { AudioManager.Sound.valueOf(name) } catch (e: Exception) { null }
            }.toMutableSet()
        }
        presetSoundsMap = map
        pinnedSounds.value = map[activePreset]?.toMutableSet() ?: mutableSetOf()
    }

    // 切换预设时更新 pinnedSounds
    LaunchedEffect(activePreset, presetSoundsMap) {
        pinnedSounds.value = presetSoundsMap[activePreset]?.toMutableSet() ?: mutableSetOf()
    }

    // 保存当前预设声音到 Preferences
    LaunchedEffect(pinnedSounds.value) {
        val newMap = presetSoundsMap.toMutableMap()
        newMap[activePreset] = pinnedSounds.value.toMutableSet()
        presetSoundsMap = newMap
        PreferencesManager.savePresetLocalPinned(context, activePreset, pinnedSounds.value.map { it.name }.toSet())
    }

    LaunchedEffect(activePreset) {
        PreferencesManager.saveActivePreset(context, activePreset)
    }

    val audioManager = remember { AudioManager.getInstance() }
    val hasPlayingSounds = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        mainViewModel.hasAnyPlayingSounds.collect { isPlaying ->
            if (isPlaying != hasPlayingSounds.value) {
                hasPlayingSounds.value = isPlaying
                if (isPlaying) {
                    Logger.d("MainScreen", "检测到音频播放，启动MusicService")
                    audioManager.startMusicService(context)
                } else {
                    Logger.d("MainScreen", "所有音频已停止")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Logger.d("MainScreen", "MainScreen onDispose")
            if (!audioManager.hasAnyPlayingSounds()) {
                audioManager.stopMusicService(context)
            }
        }
    }

    // 检查所有预设是否有声音（决定是否显示预设模块）
    var allRemotePinned by remember { mutableStateOf(PreferencesManager.getAllPresetRemotePinned(context)) }
    LaunchedEffect(Unit) {
        mainViewModel.allPresetRemotePinned.collect { pinned ->
            allRemotePinned = pinned
        }
    }

    val defaultAreaHasSounds = remember {
        derivedStateOf {
            val hasLocal = presetSoundsMap.values.any { it.isNotEmpty() }
            hasLocal || allRemotePinned.isNotEmpty()
        }
    }

    LaunchedEffect(defaultAreaHasSounds.value, presetSoundsMap, allRemotePinned) {
        Logger.d("MainScreen", "预设模块显示状态变化: $defaultAreaHasSounds, 预设数=${presetList.size}, 远程固定=${allRemotePinned.size}")
    }

    // 更新 ViewModel 中当前预设的声音列表
    LaunchedEffect(activePreset, pinnedSounds.value) {
        mainViewModel.updatePresetSounds(pinnedSounds.value)
    }
    
    // 监听当前路由，判断是否在二级页面
    val currentBackStackEntry by navigator.navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isInSecondaryPage = currentRoute in listOf("theme", "local_audio", "quoteHistory", "flipclock", "tomato_timer", "diary", "breathing_detail/{methodId}", "meditation/{categoryId}/{sessionId}", "meditation/{categoryId}")
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
                    customBackgroundUri = customBackgroundUri,
                    customBackgroundColor = customBackgroundColor,
                    backgroundOpacity = backgroundOpacity,
                    backgroundBlurRadius = backgroundBlurRadius,
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
                if (simpleMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {},
                                    onDragCancel = {}
                                ) { _, _ -> }
                            }
                    ) {
                        org.xmsleep.app.ui.starsky.StarSkySimpleContent(
                            modifier = Modifier.fillMaxSize(),
                            onExitSimpleMode = { simpleMode = false }
                        )
                    }
                    return@composable
                }
                // 主页面：直接根据 selectedItem 切换内容（支持左右滑动切换）
                AnimatedContent(
                    targetState = selectedItem,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var accumulatedDrag = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val threshold = size.width * 0.35f
                                    when {
                                        accumulatedDrag > threshold -> {
                                            when (selectedItem) {
                                                2 -> selectedItem = 1
                                                5 -> selectedItem = 2
                                                4 -> selectedItem =
                                                    if (showRadioTab) 5 else 2
                                                3 -> selectedItem =
                                                    if (showBreathingTab) 4
                                                    else if (showRadioTab) 5
                                                    else 2
                                            }
                                        }
                                        accumulatedDrag < -threshold -> {
                                            when (selectedItem) {
                                                1 -> selectedItem = 2
                                                2 -> selectedItem =
                                                    if (showRadioTab) 5
                                                    else if (showBreathingTab) 4
                                                    else 3
                                                5 -> selectedItem =
                                                    if (showBreathingTab) 4 else 3
                                                4 -> selectedItem = 3
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
                        val tabOrder = mapOf(1 to 0, 2 to 1, 5 to 2, 4 to 3, 3 to 4)
                        val fromPos = tabOrder[initialState] ?: 0
                        val toPos = tabOrder[targetState] ?: 0
                        val direction = if (toPos > fromPos) 1 else -1
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
                                presetList = presetList,
                                onPresetListChange = { presetList = it },
                                presetSoundsMap = presetSoundsMap,
                                pinnedSounds = pinnedSounds,
                                activePreset = activePreset,
                                onActivePresetChange = { newPreset -> activePreset = newPreset },
                                hasAnyPresetItems = defaultAreaHasSounds.value,
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
                                onShowDeveloperLetter = { showDeveloperLetter = true },
                                updateViewModel = updateViewModel,
                                soundsViewModel = soundsViewModel
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
                        5 -> {
                            // 电台页面
                            org.xmsleep.app.ui.radio.RadioScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            )
                        }
                        4 -> {
                            // 呼吸列表页面
                            BreathingListScreen(
                                onMethodClick = { methodId ->
                                    navigator.navigateToBreathingDetail(methodId)
                                },
                                onMeditationClick = { categoryId ->
                                    navigator.navigateToMeditation(categoryId)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
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
                                backgroundOpacity = backgroundOpacity,
                                backgroundBlurRadius = backgroundBlurRadius,
                                onBackgroundOpacityChange = onBackgroundOpacityChange,
                                onBackgroundBlurRadiusChange = onBackgroundBlurRadiusChange,
                                updateViewModel = updateViewModel,
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChange,
                                customBackgroundUri = customBackgroundUri,
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
                                onNavigateToQuoteHistory = {
                                    navigator.navigateToQuoteHistory()
                                },
                                onNavigateToFlipClock = {
                                    navigator.navigateToFlipClock()
                                },
                                onNavigateToDiary = {
                                    navigator.navigateToDiary()
                                },
                                pinnedSounds = pinnedSounds,
                                locationPermissionLauncher = locationPermissionLauncher,
                                onShowDeveloperLetter = { showDeveloperLetter = true },
                                onEnterSimpleMode = { simpleMode = true },
                                onPickCustomBackground = onPickCustomBackground,
                                customBackgroundThumbnail = customBackgroundThumbnail,
                                pendingCustomBgUri = pendingCustomBgUri,
                                pendingCustomBgThumbnail = pendingCustomBgThumbnail,
                                pendingCustomBgColor = pendingCustomBgColor,
                                onCommitCustomBg = onCommitCustomBg,
                                onCancelCustomBg = onCancelCustomBg,
                                onCustomColorChange = onCustomColorChange
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
                    onBack = { navigator.popBackStack() },
                    activity = mainActivity
                )
            }
            
            composable("tomato_timer") {
                TomatoTimerScreen(
                    onBack = { navigator.popBackStack() }
                )
            }

            composable("diary") {
                org.xmsleep.app.diary.DiaryScreen(
                    onBack = { navigator.popBackStack() },
                    onScrollDetected = {
                        shouldCollapseFloatingButton = true
                    }
                )
            }
            
            composable("breathing_detail/{methodId}") { backStackEntry ->
                val methodId = backStackEntry.arguments?.getString("methodId") ?: "sleep_478"
                BreathingDetailScreen(
                    methodId = methodId,
                    activity = mainActivity,
                    onBack = { navigator.popBackStack() }
                )
            }

            composable("meditation/{categoryId}/{sessionId}") { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "meditation_sleep"
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                org.xmsleep.app.ui.meditation.MeditationPlayerScreen(
                    categoryId = categoryId,
                    sessionId = sessionId,
                    onBack = { navigator.popBackStack() }
                )
            }

            composable("meditation/{categoryId}") { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: "meditation_sleep"
                org.xmsleep.app.ui.meditation.MeditationPlayerScreen(
                    categoryId = categoryId,
                    onBack = { navigator.popBackStack() }
                )
            }
        }
                }
            }
            
            // 底部导航栏 - 作为独立层，应用毛玻璃效果
            // 只在主页面显示，且设置页面内容未隐藏时显示
            androidx.compose.animation.AnimatedVisibility(
                visible = isMainRoute && !isSettingsContentHidden && !simpleMode,
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
                            onLongClick = { showDeveloperLetter = true },
                            icon = Icons.Default.LocalFlorist,
                            label = context.getString(R.string.tab_white_noise),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 繁星 Tab
                        NavigationBarItem(
                            selected = selectedItem == 2,
                            onClick = { selectedItem = 2 },
                            icon = Icons.Default.Satellite,
                            label = context.getString(R.string.tab_starsky),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 电台 Tab
                        if (showRadioTab) {
                        NavigationBarItem(
                            selected = selectedItem == 5,
                            onClick = { selectedItem = 5 },
                            icon = Icons.Default.Radio,
                            label = context.getString(R.string.tab_radio),
                            modifier = Modifier.weight(1f)
                        )
                        }
                        
                        // 呼吸 Tab
                        if (showBreathingTab) {
                        NavigationBarItem(
                            selected = selectedItem == 4,
                            onClick = { selectedItem = 4 },
                            icon = Icons.Default.SelfImprovement,
                            label = context.getString(R.string.tab_breathing),
                            modifier = Modifier.weight(1f)
                        )
                        }
                        
                        // 设置 Tab
                        NavigationBarItem(
                            selected = selectedItem == 3,
                            onClick = { selectedItem = 3 },
                            icon = Icons.Default.Settings,
                            label = context.getString(R.string.tab_settings),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 全局浮动播放按钮（新版本 - 吸附式交互）
        org.xmsleep.app.ui.FloatingPlayButtonNew(
            audioManager = audioManager,
            selectedTab = if (simpleMode) 2 else selectedItem, // 简化模式视为繁星页
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
                val newLocalSet = pinnedSounds.value.toMutableSet()
                newLocalSet.addAll(localSounds)
                pinnedSounds.value = newLocalSet

                val currentRemotePinned = PreferencesManager.getPresetRemotePinned(context, activePreset)
                val newRemoteSet = currentRemotePinned.toMutableSet()
                newRemoteSet.addAll(remoteSoundIds)
                PreferencesManager.savePresetRemotePinned(context, activePreset, newRemoteSet)

                val addedCount = localSounds.size + remoteSoundIds.size
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.added_to_preset, addedCount, activePreset),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
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
                    val soundsToPlay = pinnedSounds.value
                    val remotePinned = PreferencesManager.getPresetRemotePinned(context, activePreset)

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

        // 开发者来信弹窗
        if (showDeveloperLetter) {
            DeveloperLetterDialog(
                onDismiss = { showDeveloperLetter = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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

        if (label != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private var hasCheckedRecentPlayOnLaunch = false

@Composable
private fun DeveloperLetterDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = context.getString(R.string.developer_letter_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = context.getString(R.string.developer_letter_content),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok))
            }
        }
    )
}
