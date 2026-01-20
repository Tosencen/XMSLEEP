package org.xmsleep.app.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
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
import org.xmsleep.app.update.UpdateDialog
import androidx.compose.ui.graphics.Color

/**
 * 主屏幕 - 包含底部导航和页面切换
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    hideAnimation: Boolean,
    soundCardsColumnsCount: Int,
    currentLanguage: LanguageManager.Language,
    audioPermissionLauncher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    onAudioPermissionGranted: () -> Unit,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onHideAnimationChange: (Boolean) -> Unit,
    onSoundCardsColumnsCountChange: (Int) -> Unit
) {
    // 使用Navigator接口来管理导航
    val navigator = rememberXMSleepNavigator()
    var selectedItem by remember { mutableIntStateOf(1) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
            android.util.Log.d("UpdateCheck", "读取到的版本号: $version")
            version
        } catch (e: Exception) {
            android.util.Log.e("UpdateCheck", "读取版本号失败，使用默认值", e)
            "0.0.0" // 使用最低版本号作为默认值，确保能检测到更新
        }
    }
    
    // 每次进入主页时静默检查更新（不自动弹窗）
    LaunchedEffect(Unit) {
        android.util.Log.d("UpdateCheck", "开始后台静默检查更新，当前版本: $currentVersion")
        // 静默检查更新，不弹窗，只更新状态和显示图标
        updateViewModel.startAutomaticCheckLatestVersion(currentVersion)
    }
    
    // 监听生命周期，当应用恢复时检查更新和待安装的文件
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                android.util.Log.d("UpdateCheck", "Activity resumed, 检查更新和待安装文件")
                
                // 先检查是否有待安装的文件且权限已授予
                if (updateViewModel.checkPendingInstall()) {
                    android.util.Log.d("UpdateCheck", "检测到待安装文件且权限已授予")
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
    
    // 初始化收藏声音，从SharedPreferences读取保存的数据
    val favoriteSounds = remember { 
        mutableStateOf(
            org.xmsleep.app.preferences.PreferencesManager.getRemoteFavorites(context)
                .mapNotNull { soundName ->
                    try { org.xmsleep.app.audio.AudioManager.Sound.valueOf(soundName) } catch (e: Exception) { null }
                }.toMutableSet()
        )
    }
    
    // 在应用退出前保存收藏数据
    DisposableEffect(Unit) {
        onDispose {
            org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(
                context,
                favoriteSounds.value.map { it.name }.toSet()
            )
        }
    }
    
    // 监听收藏声音的变化，保存到SharedPreferences
    LaunchedEffect(favoriteSounds.value) {
        // 注意：这里直接使用 RemoteFavorites 存储是为了兼容远程音频的存储方式
        // 实际应该有单独的本地收藏存储，但当前系统混用了远程和本地
        org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(
            context, 
            favoriteSounds.value.map { it.name }.toSet()
        )
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
                    android.util.Log.d("MainScreen", "检测到音频播放，启动MusicService")
                    audioManager.startMusicService(context)
                } else {
                    // 所有音频已停止
                    android.util.Log.d("MainScreen", "所有音频已停止")
                }
            }
            delay(1000) // 每秒检查一次
        }
    }
    
    // 应用退出时清理服务
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("MainScreen", "MainScreen onDispose")
            // 如果没有播放中的声音，停止服务
            if (!audioManager.hasAnyPlayingSounds()) {
                audioManager.stopMusicService(context)
            }
        }
    }
    
    // PreferencesManager实例（用于管理预设的远程声音）
    val preferencesManager = remember { org.xmsleep.app.preferences.PreferencesManager }
    
    // 应用启动时检查 SharedPreferences 中的收藏数据，确保数据一致
    DisposableEffect(Unit) {
        val savedFavorites = org.xmsleep.app.preferences.PreferencesManager.getRemoteFavorites(context)
            .mapNotNull { soundName ->
                try { org.xmsleep.app.audio.AudioManager.Sound.valueOf(soundName) } catch (e: Exception) { null }
            }.toMutableSet()
        
        // 如果读取到的数据与当前数据不一致，说明应用被关闭后重新打开，需要同步
        if (savedFavorites.isNotEmpty() && favoriteSounds.value.isEmpty()) {
            favoriteSounds.value = savedFavorites
            android.util.Log.d("MainScreen", "从SharedPreferences恢复收藏数据: ${savedFavorites.size}个")
        }
        
        onDispose { /* 不需要清理 */ }
    }
    
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
        android.util.Log.d("MainScreen", "预设模块显示状态变化: $defaultAreaHasSounds, 本地预设1=${preset1Sounds.value.size}, 预设2=${preset2Sounds.value.size}, 预设3=${preset3Sounds.value.size}, 远程固定=${allRemotePinned.size}")
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
                android.util.Log.d("MainScreen", "所有预设远程音频固定状态变化: ${allRemotePinned.size} -> ${newAllRemotePinned.size}")
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
    val isInSecondaryPage = currentRoute in listOf("theme", "favorite", "local_audio", "quoteHistory")
    val isMainRoute = !isInSecondaryPage  // 主页面 = 不在二级页面
    
    // 使用ProvideNavigator提供导航器给子组件
    ProvideNavigator(navigator) {
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            // TopBar已移除（视频源功能已删除）
        },
        floatingActionButton = {
            // FloatingActionButton已移除（视频源功能已删除）
        },
        bottomBar = {
            // 只在主页面显示底部导航栏
            AnimatedVisibility(
                visible = isMainRoute,
                exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                enter = fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = 50))
            ) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LocalFlorist, null) },
                        label = { Text(context.getString(R.string.white_noise)) },
                        selected = selectedItem == 1,
                        onClick = { selectedItem = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Satellite, null) },
                        label = { Text(context.getString(R.string.star_sky)) },
                        selected = selectedItem == 2,
                        onClick = { selectedItem = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text(context.getString(R.string.settings)) },
                        selected = selectedItem == 3,
                        onClick = { selectedItem = 3 }
                    )
                }
            }
        }
    ) { paddingValues ->
        // NavHost 始终存在，用于处理二级页面导航
        NavHost(
            navController = navigator.navController,
            startDestination = "main",  // 主页面路由
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
                                                2 -> selectedItem = 1  // 从星空到白噪音
                                                3 -> selectedItem = 2  // 从设置到星空
                                            }
                                        }
                                        // 向左滑动
                                        accumulatedDrag < -threshold -> {
                                            when (selectedItem) {
                                                1 -> selectedItem = 2  // 从白噪音到星空
                                                2 -> selectedItem = 3  // 从星空到设置
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
                        val direction = if (targetState > initialState) 1 else -1
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
                                columnsCount = soundCardsColumnsCount,
                                onColumnsCountChange = onSoundCardsColumnsCountChange,
                                preset1Sounds = preset1Sounds,
                                preset2Sounds = preset2Sounds,
                                preset3Sounds = preset3Sounds,
                                favoriteSounds = favoriteSounds,
                                activePreset = activePreset,
                                onActivePresetChange = { newPreset -> activePreset = newPreset },
                                hasAnyPresetItems = defaultAreaHasSounds,
                                onNavigateToFavorite = {
                                    navigator.navigateToFavorite()
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
                        3 -> {
                            SettingsScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                hideAnimation = hideAnimation,
                                onHideAnimationChange = onHideAnimationChange,
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
                                pinnedSounds = pinnedSounds,
                                favoriteSounds = favoriteSounds
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
            
            composable("favorite") {
                org.xmsleep.app.ui.FavoriteScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    hideAnimation = hideAnimation,
                    columnsCount = 3, // 收藏页面默认3列
                    pinnedSounds = pinnedSounds,
                    favoriteSounds = favoriteSounds,
                    onBack = { navigator.popBackStack() },
                    onScrollDetected = {
                        // 滚动时收缩悬浮按钮
                        shouldCollapseFloatingButton = true
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(100)
                            shouldCollapseFloatingButton = false
                        }
                    },
                    onPinnedChange = { sound, isPinned ->
                        val currentSet = pinnedSounds.value.toMutableSet()
                        if (isPinned) {
                            // 检查是否已达到最大数量（3个）
                            if (currentSet.size >= 3) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(R.string.max_3_sounds_limit),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                currentSet.add(sound)
                                pinnedSounds.value = currentSet
                            }
                        } else {
                            currentSet.remove(sound)
                            pinnedSounds.value = currentSet
                        }
                    },
                    onFavoriteChange = { sound, isFavorite ->
                        val currentSet = favoriteSounds.value.toMutableSet()
                        if (isFavorite) {
                            currentSet.add(sound)
                        } else {
                            currentSet.remove(sound)
                        }
                        favoriteSounds.value = currentSet
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
                    onBack = { navigator.popBackStack() }
                )
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
        
        // 最近播放弹窗 - 只在应用启动时显示一次
        var showRecentPlayDialog by remember { mutableStateOf(false) }
        var hasCheckedRecentPlay by remember { mutableStateOf(false) }
        
        // 只在应用启动时检查一次是否显示弹窗
        LaunchedEffect(Unit) {
            if (!hasCheckedRecentPlay) {
                hasCheckedRecentPlay = true
                val audioManager = org.xmsleep.app.audio.AudioManager.getInstance()
                // 检查是否有最近播放记录
                if (audioManager.hasRecentSounds(context)) {
                    // 延迟500ms显示，确保UI已完全加载
                    delay(500)
                    showRecentPlayDialog = true
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
        }
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
                        android.util.Log.d("MainScreen", "点击授予权限按钮")
                        showPermissionDialog = false
                        // 使用从 MainActivity 传递的 audioPermissionLauncher 请求权限
                        audioPermissionLauncher.launch(requiredPermission)
                        permissionGrantedPending = true
                        onAudioPermissionGranted()
                        android.util.Log.d("MainScreen", "权限请求已发送: $requiredPermission")
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
