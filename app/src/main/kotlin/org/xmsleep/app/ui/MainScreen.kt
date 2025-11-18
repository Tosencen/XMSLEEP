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
    
    // 用于触发浮动按钮收缩的状态
    var shouldCollapseFloatingButton by remember { mutableStateOf(false) }
    
    // 用于强制收缩悬浮播放按钮的状态（当底部预设模块展开时）
    var forceCollapseFloatingButton by remember { mutableStateOf(false) }
    
    // 自动更新检查（全局共享）
    val updateViewModel = remember { org.xmsleep.app.update.UpdateViewModel(context) }
    val updateState by updateViewModel.updateState.collectAsState()
    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    
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
    
    // 每次进入主页时自动检查更新（使用LaunchedEffect确保只在组合时执行一次）
    LaunchedEffect(Unit) {
        android.util.Log.d("UpdateCheck", "开始自动检查更新，当前版本: $currentVersion")
        
        // 先检查初始状态，如果已经是HasUpdate状态，立即显示弹窗（处理从后台恢复的情况）
        val initialState = updateViewModel.updateState.value
        if (initialState is org.xmsleep.app.update.UpdateState.HasUpdate) {
            android.util.Log.d("UpdateCheck", "初始状态已是HasUpdate，显示弹窗")
            delay(500)
            showAutoUpdateDialog = true
                        } else {
            // 否则开始检查更新
            updateViewModel.startAutomaticCheckLatestVersion(currentVersion)
        }
    }
    
    // 当检测到有新版本时，自动显示更新弹窗
    // 使用LaunchedEffect监听updateState变化，并在状态为HasUpdate时显示弹窗
    LaunchedEffect(updateState) {
        val state = updateState
        when (state) {
            is org.xmsleep.app.update.UpdateState.HasUpdate -> {
                android.util.Log.d("UpdateCheck", "检测到新版本: ${state.version.version}")
                // 延迟一小段时间确保UI已准备好
                delay(300)
                showAutoUpdateDialog = true
            }
            is org.xmsleep.app.update.UpdateState.CheckFailed -> {
                android.util.Log.e("UpdateCheck", "检查更新失败: ${state.error}")
                // 如果是rate limit错误，也显示弹窗提示用户
                if (state.error.contains("rate limit", ignoreCase = true) || 
                    state.error.contains("请求次数", ignoreCase = true)) {
                    android.util.Log.d("UpdateCheck", "Rate limit错误，显示弹窗提示用户")
                    delay(300)
                    showAutoUpdateDialog = true
                }
            }
            is org.xmsleep.app.update.UpdateState.UpToDate -> {
                android.util.Log.d("UpdateCheck", "当前已是最新版本")
            }
            is org.xmsleep.app.update.UpdateState.Checking -> {
                android.util.Log.d("UpdateCheck", "正在检查更新...")
            }
            else -> {}
        }
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
                    android.util.Log.d("UpdateCheck", "检测到待安装文件且权限已授予，自动弹出安装对话框")
                    // 在协程中延迟一小段时间确保UI已准备好
                    scope.launch {
                        delay(500)
                        // 自动重试安装
                        updateViewModel.autoRetryInstall()
                        // 显示更新对话框（如果还没有显示）
                        if (!showAutoUpdateDialog) {
                            showAutoUpdateDialog = true
                        }
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
    
    val favoriteSounds = remember { mutableStateOf(mutableSetOf<org.xmsleep.app.audio.AudioManager.Sound>()) }
    
    // AudioManager实例（用于播放/暂停快捷播放的声音）
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    
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
    val isInSecondaryPage = currentRoute in listOf("theme", "favorite")
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
                                darkMode = darkMode,
                                onDarkModeChange = onDarkModeChange,
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
                                }
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
                    onBack = { navigator.popBackStack() }
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
        }
        }
        
        // 全局浮动播放按钮（新版本 - 吸附式交互）
        org.xmsleep.app.ui.FloatingPlayButtonNew(
            audioManager = audioManager,
            selectedTab = selectedItem, // 传递当前选中的 tab
            shouldCollapse = shouldCollapseFloatingButton, // 传递收缩标志
            activePreset = activePreset,
            forceCollapse = forceCollapseFloatingButton, // 传递强制收缩标志
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
        }
    }
    
    // 自动更新弹窗（检测到新版本时自动显示）
    // 注意：只在HasUpdate状态时显示自动弹窗，避免在Idle状态时重复检查
    val currentUpdateState by updateViewModel.updateState.collectAsState()
    
    // 判断是否应该显示更新弹窗
    // 在以下状态时显示：HasUpdate、Downloading、Downloaded、DownloadFailed、Installing、InstallFailed
    val shouldShowUpdateDialog = when (currentUpdateState) {
        is org.xmsleep.app.update.UpdateState.HasUpdate -> {
            android.util.Log.d("UpdateDialog", "状态: HasUpdate, 应该显示弹窗")
            true
        }
        is org.xmsleep.app.update.UpdateState.Downloading -> {
            val downloadingState = currentUpdateState as org.xmsleep.app.update.UpdateState.Downloading
            android.util.Log.d("UpdateDialog", "状态: Downloading, 应该显示弹窗, 进度: ${downloadingState.progress}")
            true
        }
        is org.xmsleep.app.update.UpdateState.Downloaded -> {
            android.util.Log.d("UpdateDialog", "状态: Downloaded, 应该显示弹窗")
            true
        }
        is org.xmsleep.app.update.UpdateState.DownloadFailed -> {
            android.util.Log.d("UpdateDialog", "状态: DownloadFailed, 应该显示弹窗")
            true
        }
        is org.xmsleep.app.update.UpdateState.Installing -> {
            android.util.Log.d("UpdateDialog", "状态: Installing, 应该显示弹窗")
            true
        }
        is org.xmsleep.app.update.UpdateState.InstallFailed -> {
            android.util.Log.d("UpdateDialog", "状态: InstallFailed, 应该显示弹窗")
            true
        }
        is org.xmsleep.app.update.UpdateState.CheckFailed -> {
            // 只在rate limit错误时显示
            val errorState = currentUpdateState as org.xmsleep.app.update.UpdateState.CheckFailed
            val shouldShow = errorState.error.contains("rate limit", ignoreCase = true) || 
                           errorState.error.contains("请求次数", ignoreCase = true)
            android.util.Log.d("UpdateDialog", "状态: CheckFailed, 应该显示弹窗: $shouldShow")
            shouldShow
        }
        else -> {
            android.util.Log.d("UpdateDialog", "状态: ${currentUpdateState::class.simpleName}, 不显示弹窗")
            false
        }
    }
    
    // 显示更新弹窗
    android.util.Log.d("UpdateDialog", "showAutoUpdateDialog: $showAutoUpdateDialog, shouldShowUpdateDialog: $shouldShowUpdateDialog")
    if (showAutoUpdateDialog && shouldShowUpdateDialog) {
        UpdateDialog(
            onDismiss = {
                // 只有在非下载/安装状态时才允许关闭弹窗
                val state = updateViewModel.updateState.value
                when (state) {
                    is org.xmsleep.app.update.UpdateState.Downloading -> {
                        // 下载中不允许关闭，由UpdateDialog内部处理
                        android.util.Log.d("UpdateCheck", "下载中，不允许关闭弹窗")
                    }
                    is org.xmsleep.app.update.UpdateState.Installing -> {
                        // 安装中不允许关闭
                        android.util.Log.d("UpdateCheck", "安装中，不允许关闭弹窗")
                    }
                    else -> {
                        showAutoUpdateDialog = false
                        android.util.Log.d("UpdateCheck", "用户关闭了更新弹窗")
                    }
                }
            },
            updateViewModel = updateViewModel,
            currentLanguage = currentLanguage
        )
    }
}
