package org.xmsleep.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.heightIn
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.lerp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.res.painterResource
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.dynamicDarkColorScheme
import kotlinx.serialization.json.*
import androidx.compose.material3.dynamicLightColorScheme
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.navigation.compose.composable
import org.xmsleep.app.update.UpdateDialog
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.navigation.XMSleepNavigator
import org.xmsleep.app.navigation.LocalNavigator
import org.xmsleep.app.navigation.rememberXMSleepNavigator
import org.xmsleep.app.navigation.ProvideNavigator
import org.xmsleep.app.R
import org.xmsleep.app.ui.AudioVisualizer

class MainActivity : ComponentActivity() {
    private var statsCollector: org.xmsleep.app.stats.StatsCollector? = null
    
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageManager.updateAppLanguage(it) })
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 在应用启动时迁移旧版本的数据（如果存在）
        org.xmsleep.app.preferences.PreferencesManager.migrateFromOldVersion(this)
        
        // 初始化统计数据收集器
        initStatsCollector()
        
        setContent {
            XMSLEEPApp()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 记录应用启动（每次进入前台）
        statsCollector?.recordLaunch()
    }
    
    override fun onPause() {
        super.onPause()
        // 记录会话结束（应用进入后台）
        statsCollector?.recordSessionEnd()
    }
    
    /**
     * 初始化统计数据收集器
     */
    private fun initStatsCollector() {
        try {
            // 从 BuildConfig 读取 GitHub Token（如果配置了）
            val buildConfigClass = Class.forName("org.xmsleep.app.BuildConfig")
            val tokenField = buildConfigClass.getField("GITHUB_TOKEN")
            val githubToken = tokenField.get(null) as? String
            
            if (!githubToken.isNullOrBlank()) {
                statsCollector = org.xmsleep.app.stats.StatsCollector(
                    context = this,
                    githubToken = githubToken,
                    gistId = null  // 首次创建，后续会自动保存Gist ID
                )
                android.util.Log.d("MainActivity", "统计数据收集器已初始化")
            } else {
                android.util.Log.d("MainActivity", "GitHub Token未配置，统计数据收集功能已禁用")
            }
        } catch (e: Exception) {
            android.util.Log.d("MainActivity", "无法读取GITHUB_TOKEN，统计数据收集功能已禁用: ${e.message}")
        }
    }
}

enum class DarkModeOption {
    LIGHT, DARK, AUTO
}

// Shape 对象必须在顶层定义（在所有函数之前）
object TopLeftDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            },
        )
    }
}

object BottomRightDiagonalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            Path().apply {
                moveTo(size.width, size.height)
                lineTo(0f, size.height)
                lineTo(size.width, 0f)
                close()
            },
        )
    }
}

@Composable
fun XMSLEEPApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 语言状态管理
    var currentLanguage by remember { mutableStateOf(LanguageManager.getCurrentLanguage(context)) }
    val localizedContext = remember(currentLanguage) {
        LanguageManager.createLocalizedContext(context, currentLanguage)
    }
    
    // 请求存储权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限请求结果处理
        android.util.Log.d("MainActivity", "存储权限请求结果: $permissions")
    }
    
    LaunchedEffect(Unit) {
        // 检查并请求存储权限
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            // Android 10及以下需要存储权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    // 调色板颜色列表（提取为共享常量）
    val paletteColors = remember {
        listOf(
            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青（第一个）
            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
            Color(0xFF4F378B),  // 默认紫色
            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
        )
    }
    
    // 主题状态管理（从SharedPreferences加载保存的设置）
    var darkMode by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getDarkMode(context))
    }
    var selectedColor by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getSelectedColor(context, paletteColors.first()))
    }
    var useDynamicColor by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getUseDynamicColor(context))
    }
    var useBlackBackground by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getUseBlackBackground(context))
    }
    var hideAnimation by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getHideAnimation(context))
    }
    var soundCardsColumnsCount by remember { 
        mutableIntStateOf(org.xmsleep.app.preferences.PreferencesManager.getSoundCardsColumnsCount(context))
    }
    
    // 计算是否使用深色主题
    val isDark = when (darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> isSystemInDarkTheme()
    }
    
    // 创建主题
    XMSLEEPTheme(
        isDark = isDark,
        seedColor = selectedColor,
        useDynamicColor = useDynamicColor,
        useBlackBackground = useBlackBackground
    ) {
        // 使用CompositionLocalProvider提供语言化的Context
        CompositionLocalProvider(
            LocalContext provides localizedContext
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen(
                    darkMode = darkMode,
                    selectedColor = selectedColor,
                    useDynamicColor = useDynamicColor,
                    useBlackBackground = useBlackBackground,
                    hideAnimation = hideAnimation,
                    soundCardsColumnsCount = soundCardsColumnsCount,
                    currentLanguage = currentLanguage,
                    onLanguageChange = { currentLanguage = it },
                    onDarkModeChange = { 
                        darkMode = it
                        org.xmsleep.app.preferences.PreferencesManager.saveDarkMode(context, it)
                    },
                    onColorChange = { 
                        selectedColor = it
                        org.xmsleep.app.preferences.PreferencesManager.saveSelectedColor(context, it)
                    },
                    onDynamicColorChange = { 
                        useDynamicColor = it
                        org.xmsleep.app.preferences.PreferencesManager.saveUseDynamicColor(context, it)
                    },
                    onBlackBackgroundChange = { 
                        useBlackBackground = it
                        org.xmsleep.app.preferences.PreferencesManager.saveUseBlackBackground(context, it)
                    },
                    onHideAnimationChange = { 
                        hideAnimation = it
                        org.xmsleep.app.preferences.PreferencesManager.saveHideAnimation(context, it)
                    },
                    onSoundCardsColumnsCountChange = { 
                        soundCardsColumnsCount = it
                        org.xmsleep.app.preferences.PreferencesManager.saveSoundCardsColumnsCount(context, it)
                    }
                )
            }
        }
    }
}

@Composable
fun XMSLEEPTheme(
    isDark: Boolean,
    seedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 设置系统状态栏样式
    val activity = context as? ComponentActivity
    DisposableEffect(activity, isDark) {
        if (activity != null) {
            if (isDark) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                )
            } else {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                    navigationBarStyle = SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ),
                )
            }
        }
        onDispose { }
    }
    
    // 使用 MaterialKolor 生成完整的配色方案
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ 使用系统动态颜色（使用原生 Material3 动态颜色 API）
        if (isDark) {
            val baseScheme = dynamicDarkColorScheme(context)
            if (useBlackBackground) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1C1C1C)
                )
            } else {
                baseScheme
            }
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        // 使用自定义主题色
        dynamicColorScheme(
            primary = seedColor,
            isDark = isDark,
            isAmoled = useBlackBackground,  // 纯黑背景
            style = PaletteStyle.TonalSpot,  // Material 3 推荐的风格
            modifyColorScheme = { scheme ->
                if (useBlackBackground && isDark) {
                    // 高对比度：纯黑背景
                    scheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceVariant = Color(0xFF1C1C1C),
                        surfaceContainer = Color(0xFF121212),
                        surfaceContainerLow = Color(0xFF0A0A0A),
                        surfaceContainerHigh = Color(0xFF1F1F1F)
                    )
                } else {
                    scheme
                }
            }
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

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
    var selectedItem by remember { mutableIntStateOf(1) } // 默认显示白噪音页面
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 用于触发浮动按钮收缩的状态
    var shouldCollapseFloatingButton by remember { mutableStateOf(false) }
    
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
    
    // 置顶和收藏状态管理（提升到MainScreen级别，确保切换tab时状态不丢失）
    val pinnedSounds = remember { mutableStateOf(mutableSetOf<org.xmsleep.app.audio.AudioManager.Sound>()) }
    val favoriteSounds = remember { mutableStateOf(mutableSetOf<org.xmsleep.app.audio.AudioManager.Sound>()) }
    
    // AudioManager实例（用于播放/暂停快捷播放的声音）
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    
    // 检查快捷播放是否有声音
    val defaultAreaHasSounds = pinnedSounds.value.isNotEmpty()
    
    // 实时检测快捷播放的播放状态（使用LaunchedEffect定期更新）
    var defaultAreaSoundsPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(pinnedSounds.value, Unit) {
        while (true) {
            defaultAreaSoundsPlaying = pinnedSounds.value.any { audioManager.isPlayingSound(it) }
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
                                pinnedSounds = pinnedSounds,
                                favoriteSounds = favoriteSounds,
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
                                }
                            )
                        }
                        2 -> {
                            // 星空页面
                            StarSkyScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
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
        
        // 全局浮动播放按钮
        org.xmsleep.app.ui.FloatingPlayButton(
            audioManager = audioManager,
            onSoundClick = { sound ->
                // 点击卡片暂停播放
                CoroutineScope(Dispatchers.Main).launch {
                    if (audioManager.isPlayingRemoteSound(sound.id)) {
                        audioManager.pauseRemoteSound(sound.id)
                    }
                }
            },
            selectedTab = selectedItem, // 传递当前选中的 tab
            shouldCollapse = shouldCollapseFloatingButton // 传递收缩标志
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = true,
    onHideAnimationChange: (Boolean) -> Unit = {},
    updateViewModel: org.xmsleep.app.update.UpdateViewModel,
    currentLanguage: LanguageManager.Language,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToSounds: () -> Unit = {},
    pinnedSounds: MutableState<MutableSet<org.xmsleep.app.audio.AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<org.xmsleep.app.audio.AudioManager.Sound>>,
    onScrollDetected: () -> Unit = {} // 滚动检测回调
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(0L) }
    var isCalculatingCache by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
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
            
            // 更新音量显示：获取第一个正在播放的声音的音量，如果没有则显示0
            val playingSounds = audioManager.getPlayingSounds()
            currentVolumeDisplay = if (playingSounds.isNotEmpty()) {
                audioManager.getVolume(playingSounds.first())
            } else {
                0f
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
            currentVolumeDisplay = if (playingSounds.isNotEmpty()) {
                audioManager.getVolume(playingSounds.first())
            } else {
                0f
            }
            delay(300) // 每300ms更新一次音量显示
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // 固定标题
        Text(
            context.getString(R.string.settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 外观设置
        Text(
            context.getString(R.string.appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
            Card(
            onClick = onNavigateToTheme,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.theme_and_colors), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.appearance_mode_theme_color),
                        style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 系统设置
        Text(
            context.getString(R.string.system),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 语言切换
        Card(
            onClick = { showLanguageDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.language), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.language_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentLanguage.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 隐藏动画文件
        SwitchItem(
            checked = hideAnimation,
            onCheckedChange = onHideAnimationChange,
            title = context.getString(R.string.hide_animation),
            description = context.getString(R.string.hide_lottie_animation_in_sound_cards)
        )
        
        // 一键调整音量
            Card(
            onClick = { showVolumeDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.adjust_all_volume), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.unified_adjust_all_sound_volume),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${(currentVolumeDisplay * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
            Card(
            onClick = { showClearCacheDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.clear_cache), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.clear_app_cache_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCalculatingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            formatBytes(cacheSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 其他
        Text(
            context.getString(R.string.other),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 软件更新
        Card(
            onClick = { showUpdateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.software_update), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.check_and_update_to_latest_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 加入群聊
        Card(
            onClick = {
                // 打开Telegram群聊链接
                val telegramUrl = "https://t.me/xmsleep"
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开链接，请检查是否安装了Telegram或浏览器", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_telegram),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column {
                        Text(context.getString(R.string.join_group), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.join_group_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 关于 XMSLEEP
        Card(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.about_xmsleep), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.view_app_info_version_copyright),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                currentLanguage = currentLanguage
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
                // 获取第一个正在播放的声音的音量作为默认值，如果没有则使用0.5
                audioManager.getPlayingSounds().firstOrNull()?.let { 
                    audioManager.getVolume(it) 
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
                                // 实时应用到所有声音
                                audioManager.getPlayingSounds().forEach { sound ->
                                    audioManager.setVolume(sound, volume)
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
                        // 应用到所有声音（包括未播放的，以便下次播放时使用）
                        listOf(
                            org.xmsleep.app.audio.AudioManager.Sound.UMBRELLA_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.ROWING,
                            org.xmsleep.app.audio.AudioManager.Sound.OFFICE,
                            org.xmsleep.app.audio.AudioManager.Sound.LIBRARY,
                            org.xmsleep.app.audio.AudioManager.Sound.HEAVY_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.TYPEWRITER,
                            org.xmsleep.app.audio.AudioManager.Sound.THUNDER_NEW,
                            org.xmsleep.app.audio.AudioManager.Sound.CLOCK,
                            org.xmsleep.app.audio.AudioManager.Sound.FOREST_BIRDS,
                            org.xmsleep.app.audio.AudioManager.Sound.DRIFTING,
                            org.xmsleep.app.audio.AudioManager.Sound.CAMPFIRE_NEW,
                            org.xmsleep.app.audio.AudioManager.Sound.WIND,
                            org.xmsleep.app.audio.AudioManager.Sound.KEYBOARD,
                            org.xmsleep.app.audio.AudioManager.Sound.SNOW_WALKING
                        ).forEach { sound ->
                            audioManager.setVolume(sound, volume)
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
    }
}

// ========== 主题设置详情页 ==========
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    darkMode: DarkModeOption,
    selectedColor: Color,
    useDynamicColor: Boolean,
    useBlackBackground: Boolean,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onColorChange: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onBlackBackgroundChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 固定 TopAppBar，不随滚动隐藏
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.theme_and_colors),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // 返回导航按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-4).dp)
                    ) {
                        Box(Modifier.size(24.dp)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = context.getString(R.string.go_back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                // TopAppBar 使用系统栏和显示区域切口
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 消费 TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 外观模式选择面板
            DarkModeSelectPanel(
                currentMode = darkMode,
                onModeSelected = onDarkModeChange
            )
            
            // 动态颜色开关（Android 12+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchItem(
                    checked = useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    title = context.getString(R.string.dynamic_color),
                    description = context.getString(R.string.use_wallpaper_color_as_theme)
                )
            }
            
            // 高对比度开关
            SwitchItem(
                checked = useBlackBackground,
                onCheckedChange = onBlackBackgroundChange,
                title = context.getString(R.string.high_contrast),
                description = context.getString(R.string.use_pure_black_background_in_dark_mode)
            )
            
            // 调色板选择
            Box(
                modifier = Modifier.alpha(if (useDynamicColor) 0.5f else 1f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(context.getString(R.string.color_palette), style = MaterialTheme.typography.titleMedium)
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 使用 HCT 色彩空间生成均匀分布的颜色
                        // 所有颜色统一使用 Chroma=40.0, Tone=40.0，确保明度一致
                        val colors = listOf(
                            Color(Hct.from(140.0, 40.0, 40.0).toInt()),  // base=4: 绿色偏青
                            Color(Hct.from(175.0, 40.0, 40.0).toInt()),  // base=5: 青绿
                            Color(Hct.from(210.0, 40.0, 40.0).toInt()),  // base=6: 青蓝
                            Color(Hct.from(245.0, 40.0, 40.0).toInt()),  // base=7: 蓝紫
                            Color(Hct.from(280.0, 40.0, 40.0).toInt()),  // base=8: 紫红
                            Color(0xFF4F378B),  // 默认紫色
                            Color(Hct.from(315.0, 40.0, 40.0).toInt()),  // base=9: 粉红
                            Color(Hct.from(350.0, 40.0, 40.0).toInt()),  // base=10: 红
                            Color(Hct.from(35.0, 40.0, 40.0).toInt()),   // base=1: 橙黄
                            Color(Hct.from(70.0, 40.0, 40.0).toInt()),   // base=2: 黄
                            Color(Hct.from(105.0, 40.0, 40.0).toInt()),  // base=3: 黄绿
                        )
                        
                        colors.forEach { color ->
                            ColorButton(
                                baseColor = color,
                                selected = selectedColor == color && !useDynamicColor,
                                onClick = {
                                    onColorChange(color)
                                    onDynamicColorChange(false)
                                }
                            )
                        }
                    }
                }
            }
            
            // 底部留出安全空间
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ========== 深色模式选择面板 ==========
@Composable
fun DarkModeSelectPanel(
    currentMode: DarkModeOption,
    onModeSelected: (DarkModeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val panelModifier = Modifier.size(96.dp, 146.dp)
    
    val themePanelItem: @Composable (DarkModeOption) -> Unit = { mode ->
        ColorSchemePreviewItem(
            onClick = { onModeSelected(mode) },
            panel = {
                if (mode != DarkModeOption.AUTO) {
                    ThemePreviewPanel(
                        isDark = mode == DarkModeOption.DARK,
                        modifier = panelModifier,
                    )
                } else {
                    DiagonalMixedThemePreviewPanel(
                        modifier = panelModifier,
                    )
                }
            },
            text = {
                Text(
                    when (mode) {
                        DarkModeOption.LIGHT -> context.getString(R.string.light_mode)
                        DarkModeOption.DARK -> context.getString(R.string.dark_mode)
                        DarkModeOption.AUTO -> context.getString(R.string.auto_mode)
                    }
                )
            },
            selected = currentMode == mode,
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        themePanelItem(DarkModeOption.LIGHT)
        themePanelItem(DarkModeOption.DARK)
        themePanelItem(DarkModeOption.AUTO)
    }
}

@Composable
fun ColorSchemePreviewItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    panel: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.Start,
    ) {
        panel()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                RadioButton(
                    selected = selected,
                    interactionSource = interactionSource,
                    onClick = null,
                )
            }
            text()
        }
    }
}

// ========== 主题预览面板 ==========
@Composable
fun ThemePreviewPanel(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用更柔和的紫色（降低饱和度和亮度）
    val previewColor = Color(Hct.from(280.0, 36.0, 50.0).toInt())
    val colorScheme = dynamicColorScheme(
        primary = previewColor,
        isDark = isDark,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
    )
    
    Box(modifier) {
        Row(
            modifier = Modifier.fillMaxSize()
                .background(
                    color = colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = colorScheme.background, shape = RoundedCornerShape(9.dp)),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(colorScheme.tertiary),
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(32.dp)
                                .background(
                                    color = colorScheme.secondary,
                                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                                ),
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(8.dp)
                                .background(color = colorScheme.primaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(8.dp)
                                .background(color = colorScheme.secondaryContainer, shape = CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(8.dp)
                                .background(color = colorScheme.tertiaryContainer, shape = CircleShape),
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .height(height = 26.dp)
                        .background(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        )
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(color = colorScheme.primary, shape = CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .background(color = colorScheme.primaryContainer, shape = CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
fun DiagonalMixedThemePreviewPanel(
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        ThemePreviewPanel(
            isDark = false,
            modifier = Modifier
                .fillMaxSize()
                .clip(TopLeftDiagonalShape),
        )
        ThemePreviewPanel(
            isDark = true,
            modifier = Modifier
                .fillMaxSize()
                .clip(BottomRightDiagonalShape),
        )
    }
}

// ========== 开关项 ==========
@Composable
fun SwitchItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

// ========== 颜色按钮 ==========
@Composable
fun ColorButton(
    baseColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerSize by animateDpAsState(targetValue = if (selected) 28.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
    
    Surface(
        modifier = modifier
            .sizeIn(maxHeight = 80.dp, maxWidth = 80.dp, minHeight = 64.dp, minWidth = 64.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize()) {
            val hct = baseColor.toHct()
            val color1 = Color(Hct.from(hct.hue, 40.0, 80.0).toInt())
            val color2 = Color(Hct.from(hct.hue, 40.0, 90.0).toInt())
            val color3 = Color(Hct.from(hct.hue, 40.0, 60.0).toInt())
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color1) }
                    .align(Alignment.Center),
            ) {
                Surface(
                    color = color2,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp),
                ) {}
                Surface(
                    color = color3,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp),
                ) {}
                if (selected) {
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .size(containerSize)
                            .drawBehind { drawCircle(primaryContainer) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize).align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// ========== 旧组件（保留以免破坏其他地方）==========
@Composable
fun ThemeModeCard(
    option: DarkModeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (option) {
                        DarkModeOption.LIGHT -> Icons.Default.LightMode
                        DarkModeOption.DARK -> Icons.Default.DarkMode
                        DarkModeOption.AUTO -> Icons.Default.Brightness4
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Text(
                when (option) {
                    DarkModeOption.LIGHT -> context.getString(R.string.light_mode)
                    DarkModeOption.DARK -> context.getString(R.string.dark_mode)
                    DarkModeOption.AUTO -> context.getString(R.string.auto_mode)
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun EnhancedColorButton(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerSize by animateDpAsState(targetValue = if (selected) 24.dp else 0.dp)
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp)
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .drawBehind { drawCircle(color) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Surface(
                        modifier = Modifier.size(containerSize),
                        shape = CircleShape,
                        color = primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = context.getString(R.string.selected),
                                tint = onPrimaryContainer,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .drawBehind { drawCircle(color) },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = context.getString(R.string.selected),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


/**
 * 计算应用缓存大小
 */
suspend fun calculateCacheSize(context: Context): Long {
    return withContext(Dispatchers.IO) {
        var totalSize = 0L
        try {
            // 计算内部缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                totalSize += getDirectorySize(cacheDir)
            }
            
            // 计算外部缓存目录
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                totalSize += getDirectorySize(externalCacheDir)
            }
        } catch (e: Exception) {
            // 计算失败返回0
        }
        totalSize
    }
}

/**
 * 递归计算目录大小
 */
fun getDirectorySize(directory: File): Long {
    var size = 0L
    try {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
    } catch (e: Exception) {
        // 忽略错误，继续计算其他文件
    }
    return size
}

/**
 * 格式化字节数为可读格式
 */
fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * 清理应用缓存
 */
suspend fun clearApplicationCache(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            // 清理缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir)
                cacheDir.mkdirs()
            }
            
            // 清理外部缓存目录
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir)
                externalCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

/**
 * 递归删除目录
 */
fun deleteRecursive(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        fileOrDirectory.listFiles()?.forEach { child ->
            deleteRecursive(child)
        }
    }
    fileOrDirectory.delete()
}

/**
 * 缓存清理对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isClearing: Boolean
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        title = { Text(context.getString(R.string.clear_cache)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(context.getString(R.string.confirm_clear_cache))
                if (isClearing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            context.getString(R.string.clearing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        context.getString(R.string.this_will_clear_all_cache_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClearing
            ) {
                Text(context.getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearing
            ) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

/**
 * 关于对话框 - 显示应用信息、版本、版权和使用说明
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    currentLanguage: LanguageManager.Language
) {
    val composeContext = LocalContext.current
    
    // 使用LaunchedEffect安全地获取应用版本信息，当语言变化时重新获取
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCodeInt by remember { mutableIntStateOf(1) }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo: android.content.pm.PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                composeContext.packageManager.getPackageInfo(
                    composeContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                composeContext.packageManager.getPackageInfo(composeContext.packageName, 0)
            }
            versionName = packageInfo?.versionName ?: "1.0.0"
            versionCodeInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt() ?: 1
            } else {
                @Suppress("DEPRECATION")
                (packageInfo?.versionCode ?: 1)
            }
        } catch (e: Exception) {
            android.util.Log.e("AboutDialog", "Error getting package info", e)
        }
    }
    
    // 使用key确保语言变化时重新组合
    key(currentLanguage) {
        AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(composeContext.getString(R.string.about_xmsleep), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
        modifier = Modifier
            .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 应用名称和版本
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "XMSLEEP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                        )
                Text(
                        composeContext.getString(R.string.version, versionName, versionCodeInt),
                    style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 应用说明（简化版）
                Text(
                    composeContext.getString(R.string.app_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider()
                
                // 隐私政策
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        composeContext.getString(R.string.privacy_policy_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        composeContext.getString(R.string.privacy_policy),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                HorizontalDivider()
                
                // 声音来源说明（简化版）
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        composeContext.getString(R.string.sound_source_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        composeContext.getString(R.string.sound_source),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                HorizontalDivider()
                
                // 版权信息
                Text(
                    composeContext.getString(R.string.copyright),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(composeContext.getString(R.string.ok))
            }
        },
        dismissButton = null
    )
    }
}

/**
 * 语言选择弹窗
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: LanguageManager.Language,
    onLanguageSelected: (LanguageManager.Language) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(context.getString(R.string.select_language))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageManager.Language.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == currentLanguage,
                                onClick = { 
                                    if (language != currentLanguage) {
                                        onLanguageSelected(language)
                                    } else {
                                        onDismiss()
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok))
            }
        },
        dismissButton = null
    )
}

/**
 * Pager Tab Indicator Offset - 让Tab指示器跟随Pager滑动而移动
 */
fun Modifier.pagerTabIndicatorOffset(
    pagerState: PagerState,
    tabPositions: List<TabPosition>,
): Modifier = layout { measurable, constraints ->
    if (tabPositions.isEmpty()) {
        layout(constraints.maxWidth, 0) {}
    } else {
        val currentPage = minOf(tabPositions.lastIndex, pagerState.currentPage)
        val currentTab = tabPositions[currentPage]
        val previousTab = tabPositions.getOrNull(currentPage - 1)
        val nextTab = tabPositions.getOrNull(currentPage + 1)
        val fraction = pagerState.currentPageOffsetFraction
        val indicatorWidth = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.width, nextTab.width, fraction).toPx().roundToInt()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.width, previousTab.width, -fraction).toPx().roundToInt()
        } else {
            currentTab.width.toPx().roundToInt()
        }
        val indicatorOffset = if (fraction > 0 && nextTab != null) {
            lerp(currentTab.left, nextTab.left, fraction).toPx().roundToInt()
        } else if (fraction < 0 && previousTab != null) {
            lerp(currentTab.left, previousTab.left, -fraction).toPx().roundToInt()
        } else {
            currentTab.left.toPx().roundToInt()
        }
        val placeable = measurable.measure(
            Constraints(
                minWidth = indicatorWidth,
                maxWidth = indicatorWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )
        layout(constraints.maxWidth, maxOf(placeable.height, constraints.minHeight)) {
            placeable.placeRelative(
                indicatorOffset,
                maxOf(constraints.minHeight - placeable.height, 0),
            )
        }
    }
}

/**
 * 星空页面
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarSkyScreen(
    modifier: Modifier = Modifier,
    onScrollDetected: () -> Unit = {} // 滚动检测回调
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 获取当前语言（不使用remember，确保语言切换后能更新）
    val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
    val isEnglish = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.ENGLISH
    val isTraditionalChinese = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.TRADITIONAL_CHINESE
    
    // 音频资源管理器
    val resourceManager = remember { 
        org.xmsleep.app.audio.AudioResourceManager.getInstance(context) 
    }
    val audioManager = remember { 
        org.xmsleep.app.audio.AudioManager.getInstance() 
    }
    val cacheManager = remember { 
        org.xmsleep.app.audio.AudioCacheManager.getInstance(context) 
    }
    
    // 状态 - 初始化时从缓存加载，避免切换tab时重复加载
    val initialCachedManifest = remember { resourceManager.getCachedManifest() }
    var remoteSounds by remember { 
        mutableStateOf(initialCachedManifest?.sounds ?: emptyList())
    }
    var remoteCategories by remember { 
        mutableStateOf(initialCachedManifest?.categories ?: emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var downloadingSounds by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var downloadingButNoProgress by remember { mutableStateOf<Set<String>>(emptySet()) } // 正在下载但还没有收到进度
    var playingSounds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var columnsCount by remember { 
        mutableIntStateOf(org.xmsleep.app.preferences.PreferencesManager.getStarSkyColumnsCount(context)) 
    }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var selectedSoundForVolume by remember { mutableStateOf<org.xmsleep.app.audio.model.SoundMetadata?>(null) }
    var volume by remember { mutableStateOf(1f) }
    var remoteFavorites by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getRemoteFavorites(context).toMutableSet()) 
    }
    var remotePinned by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getRemotePinned(context).toMutableSet()) 
    }
    
    
    // 监听播放状态变化
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // 每500ms检查一次播放状态
            val currentlyPlaying = remoteSounds.filter { sound ->
                audioManager.isPlayingRemoteSound(sound.id)
            }.map { it.id }.toSet()
            playingSounds = currentlyPlaying
        }
    }
    
    // 加载音频清单 - 优化：先显示缓存，后台刷新，避免重复加载
    LaunchedEffect(Unit) {
        // 如果已经有数据（从缓存初始化），不重新加载（避免切换tab时重复加载）
        if (remoteSounds.isNotEmpty() && remoteCategories.isNotEmpty()) {
            android.util.Log.d("StarSkyScreen", "已有缓存数据，跳过加载，直接后台刷新")
            // 有缓存数据，延迟刷新（避免频繁请求），只在后台静默更新
            delay(5000) // 延迟5秒后刷新，避免频繁请求
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "后台刷新清单成功，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 后台刷新失败不影响显示
                android.util.Log.e("StarSkyScreen", "后台刷新音频清单失败: ${e.message}")
            }
            return@LaunchedEffect
        }
        
        // 没有缓存数据，需要加载
        errorMessage = null
        
        // 第一步：先尝试从缓存加载（同步，快速），立即显示（不显示加载状态）
        val cachedManifest = resourceManager.getCachedManifest()
        if (cachedManifest != null) {
            remoteSounds = cachedManifest.sounds
            remoteCategories = cachedManifest.categories
            android.util.Log.d("StarSkyScreen", "从缓存加载清单，分类数量: ${cachedManifest.categories.size}")
            isLoading = false // 有缓存数据，不显示加载状态
        } else {
            // 完全没有缓存，显示加载状态
            isLoading = true
        }
        
        // 第二步：后台刷新网络数据（不阻塞UI）
        if (remoteSounds.isEmpty()) {
            // 没有数据，立即刷新
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "成功刷新清单，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 刷新失败不影响显示，只记录错误
                if (remoteSounds.isEmpty()) {
                    // 只有在完全没有数据时才显示错误
                    errorMessage = e.message
                }
                android.util.Log.e("StarSkyScreen", "刷新音频清单失败: ${e.message}")
            } finally {
                isLoading = false
            }
        } else {
            // 有数据，延迟刷新（避免频繁请求），只在后台静默更新
            delay(5000) // 延迟5秒后刷新，避免频繁请求
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "后台刷新清单成功，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 后台刷新失败不影响显示
                android.util.Log.e("StarSkyScreen", "后台刷新音频清单失败: ${e.message}")
            }
        }
    }
    
    // 获取分类显示名称的辅助函数
    fun getCategoryDisplayName(categoryId: String): String {
        val category = remoteCategories.find { it.id == categoryId }
        if (category != null) {
            return when {
                isEnglish && category.nameEn != null -> category.nameEn
                isTraditionalChinese && category.nameZhTW != null -> category.nameZhTW
                isTraditionalChinese -> category.name // 如果没有繁体中文，使用简体中文
                else -> category.name
            }
        }
        
        // 如果找不到分类，使用后备映射（确保不会显示英文）
        val fallbackMap = mapOf(
            "nature" to if (isTraditionalChinese) "自然" else if (isEnglish) "Nature" else "自然",
            "rain" to if (isTraditionalChinese) "雨聲" else if (isEnglish) "Rain" else "雨声",
            "urban" to if (isTraditionalChinese) "城市" else if (isEnglish) "Urban" else "城市",
            "places" to if (isTraditionalChinese) "場所" else if (isEnglish) "Places" else "场所",
            "transport" to if (isTraditionalChinese) "交通" else if (isEnglish) "Transport" else "交通",
            "things" to if (isTraditionalChinese) "物品" else if (isEnglish) "Things" else "物品",
            "noise" to if (isTraditionalChinese) "噪音" else if (isEnglish) "Noise" else "噪音",
            "animals" to if (isTraditionalChinese) "動物" else if (isEnglish) "Animals" else "动物",
        )
        
        return fallbackMap[categoryId] ?: categoryId
    }
    
    // 获取音频显示名称的辅助函数
    fun getSoundDisplayName(sound: org.xmsleep.app.audio.model.SoundMetadata): String {
        return when {
            isEnglish && sound.nameEn != null -> sound.nameEn
            isTraditionalChinese && sound.nameZhTW != null -> sound.nameZhTW
            isTraditionalChinese -> sound.name // 如果没有繁体中文，使用简体中文
            else -> sound.name
        }
    }
    
    // 按分类分组
    val soundsByCategory = remember(remoteSounds) {
        remoteSounds.groupBy { it.category }
    }
    
    // 获取分类列表（使用分类ID，按照JSON中的order字段排序）
    val categoryIds = remember(remoteSounds, remoteCategories) {
        val categoryIdsFromSounds = remoteSounds.map { it.category }.distinct().toSet()
        // 优先使用 remoteCategories 中的 order 字段排序
        if (remoteCategories.isNotEmpty()) {
            val sorted = remoteCategories
                .filter { it.id in categoryIdsFromSounds }
                .sortedBy { it.order }
                .map { it.id }
            android.util.Log.d("StarSkyScreen", "分类排序: ${sorted.joinToString { it }}")
            android.util.Log.d("StarSkyScreen", "分类详情: ${remoteCategories.map { "${it.id}: order=${it.order}" }.joinToString()}")
            sorted
        } else {
            // 如果没有分类信息，使用字符串排序作为后备
            android.util.Log.w("StarSkyScreen", "remoteCategories 为空，使用字符串排序")
            categoryIdsFromSounds.sorted()
        }
    }
    
    // 保存每个分类的滚动状态，使用 remember 确保 tab 切换时保留
    // 使用 Map 来存储每个分类的滚动状态，key 是分类 ID（null 表示"全部"）
    val listStates = remember { mutableMapOf<String?, LazyListState>() }
    val gridStates = remember { mutableMapOf<String?, LazyGridState>() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // 标题栏（包含标题和布局切换按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.star_sky),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 布局切换按钮
            IconButton(
                onClick = { 
                    val newColumnsCount = if (columnsCount == 2) 3 else 2
                    columnsCount = newColumnsCount
                    org.xmsleep.app.preferences.PreferencesManager.saveStarSkyColumnsCount(context, newColumnsCount)
                }
            ) {
                Icon(
                    imageVector = if (columnsCount == 2) Icons.Default.GridView else Icons.Outlined.ViewAgenda,
                    contentDescription = if (columnsCount == 2) 
                        context.getString(R.string.switch_to_3_columns) 
                    else 
                        context.getString(R.string.switch_to_2_columns),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // 错误状态
        else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
        horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = context.getString(R.string.load_failed, errorMessage ?: ""),
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val manifest = resourceManager.refreshRemoteManifest().getOrNull()
                                    if (manifest != null) {
                                        remoteSounds = manifest.sounds
                                        remoteCategories = manifest.categories
                                    } else {
                                        val sounds = resourceManager.getRemoteSounds()
                                        remoteSounds = sounds
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text(context.getString(R.string.retry))
                    }
                }
            }
        }
        // 音频列表
        else if (remoteSounds.isNotEmpty()) {
            // 构建所有分类列表（包括"全部"）
            val allCategories = remember(categoryIds) { listOf(null) + categoryIds }
            
            // 使用PagerState来管理页面状态，实现左右滑动
            val initialPage = remember(allCategories) { 
                allCategories.indexOfFirst { it == selectedCategory }.coerceAtLeast(0) 
            }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { allCategories.size }
            )
            
            // 同步PagerState和selectedCategory
            LaunchedEffect(pagerState.currentPage) {
                val newCategory = allCategories.getOrNull(pagerState.currentPage)
                if (newCategory != selectedCategory) {
                    selectedCategory = newCategory
                }
            }
            
            // 同步selectedCategory和PagerState
            LaunchedEffect(selectedCategory) {
                val targetIndex = allCategories.indexOfFirst { it == selectedCategory }
                if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                    pagerState.animateScrollToPage(targetIndex)
                }
            }
            
            // 分类筛选 - 使用Tab Row样式（传统标签页样式）
            if (categoryIds.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    allCategories.forEachIndexed { index, categoryId ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                scope.launch { 
                                    pagerState.animateScrollToPage(index) 
                                }
                            },
                            text = {
                                Text(
                                    text = if (categoryId == null) 
                                        context.getString(R.string.all_categories) 
                                    else 
                                        getCategoryDisplayName(categoryId),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
            
            // 音频列表 - 使用HorizontalPager实现左右滑动
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                pageSpacing = 0.dp
            ) { pageIndex ->
                val category = allCategories[pageIndex]
                if (category == null) {
                    // "全部"类别下，按分类分组显示
                    val soundsByCategory = remoteSounds.groupBy { it.category }
                    val sortedCategories = categoryIds.filter { soundsByCategory.containsKey(it) }
                    
                    // 使用 remember 配合分类 ID 作为 key，确保 tab 切换时保留滚动状态
                    // 直接从 Map 中获取或创建状态，由于 listStates 在 remember 中保存，状态会在 tab 切换时保留
                    val lazyListState = listStates.getOrPut(null) { rememberLazyListState() }
                    
                    // 监听滚动状态，触发浮动按钮收缩
                    LaunchedEffect(lazyListState.isScrollInProgress) {
                        if (lazyListState.isScrollInProgress) {
                            onScrollDetected()
                        }
                    }
                    
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    sortedCategories.forEach { categoryId ->
                        val categorySounds = soundsByCategory[categoryId] ?: emptyList()
                        if (categorySounds.isNotEmpty()) {
                            // 分类标题
                            item(key = "category_$categoryId") {
                                Text(
                                    text = getCategoryDisplayName(categoryId),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            // 该分类下的卡片网格（使用非Lazy Grid避免嵌套滚动）
                            item(key = "grid_$categoryId") {
                                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                                val horizontalPadding = 32.dp
                                val spacing = 12.dp * (columnsCount - 1)
                                val cardWidth = (screenWidth - horizontalPadding - spacing) / columnsCount
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    categorySounds.chunked(columnsCount).forEach { rowSounds ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowSounds.forEach { sound ->
                                                Box(
                                                    modifier = Modifier.width(cardWidth)
                                                ) {
                                                    RemoteSoundCard(
                        sound = sound,
                        displayName = getSoundDisplayName(sound),
                        isPlaying = playingSounds.contains(sound.id),
                        downloadProgress = downloadingSounds[sound.id],
                        isDownloadingButNoProgress = downloadingButNoProgress.contains(sound.id),
                        columnsCount = columnsCount,
                        isPinned = remotePinned.contains(sound.id),
                        isFavorite = remoteFavorites.contains(sound.id),
                        onPinnedChange = { isPinned ->
                            val newSet = remotePinned.toMutableSet()
                            if (isPinned) {
                                // 检查音频是否已下载
                                val cachedFile = cacheManager.getCachedFile(sound.id)
                                if (cachedFile == null) {
                                    // 未下载，不允许置顶
                                    Toast.makeText(context, context.getString(R.string.must_download_before_pin), Toast.LENGTH_SHORT).show()
                                } else {
                                    // 检查是否已达到最大数量（3个）
                                    if (newSet.size >= 3) {
                                        Toast.makeText(context, context.getString(R.string.max_3_sounds_limit), Toast.LENGTH_SHORT).show()
                                    } else {
                                        newSet.add(sound.id)
                                        remotePinned = newSet
                                        org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                    }
                                }
                            } else {
                                newSet.remove(sound.id)
                                remotePinned = newSet
                                org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                            }
                        },
                        onFavoriteChange = { isFavorite ->
                            val newSet = remoteFavorites.toMutableSet()
                            if (isFavorite) {
                                newSet.add(sound.id)
                            } else {
                                newSet.remove(sound.id)
                            }
                            remoteFavorites = newSet
                            org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(context, newSet)
                        },
                        onCardClick = {
                            scope.launch {
                                try {
                                    // 首先检查是否正在播放，如果正在播放则停止播放
                                    val currentlyPlaying = audioManager.isPlayingRemoteSound(sound.id)
                                    if (currentlyPlaying) {
                                        audioManager.pauseRemoteSound(sound.id)
                                        return@launch
                                    }
                                    
                                    // 如果未播放，检查是否需要下载
                                    val cachedFile = cacheManager.getCachedFile(sound.id)
                                    if (cachedFile == null && sound.remoteUrl != null) {
                                        // 开始下载，添加到"正在下载但还没有进度"集合
                                        downloadingButNoProgress = downloadingButNoProgress + sound.id
                                        // 开始下载
                                        val downloadFlow = cacheManager.downloadAudioWithProgress(
                                            sound.remoteUrl,
                                            sound.id
                                        )
                                        downloadFlow.collect { progress ->
                                            when (progress) {
                                                is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                    val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                    android.util.Log.d("StarSkyScreen", "下载进度: ${sound.id} = $percent")
                                                    // 收到第一个进度更新，从"正在下载但还没有进度"集合中移除
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        put(sound.id, percent)
                                                    }
                                                }
                                                is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                    android.util.Log.d("StarSkyScreen", "下载完成: ${sound.id}")
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        remove(sound.id)
                                                    }
                                                    // 下载完成后播放
                                                    val uri = resourceManager.getSoundUri(sound)
                                                    if (uri != null) {
                                                        audioManager.playRemoteSound(context, sound, uri)
                                                        playingSounds = playingSounds + sound.id
                                                    } else {
                                                        android.util.Log.e("StarSkyScreen", "下载完成后无法获取URI: ${sound.id}")
                                                        Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                                    }
                                                    return@collect
                                                }
                                                is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                    android.util.Log.e("StarSkyScreen", "下载失败: ${sound.id} - ${progress.exception.message}")
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        remove(sound.id)
                                                    }
                                                    Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                                                    return@collect
                                                }
                                            }
                                        }
                                    } else {
                                        // 已缓存或直接播放
                                        val uri = resourceManager.getSoundUri(sound)
                                        if (uri != null) {
                                            audioManager.playRemoteSound(context, sound, uri)
                                            playingSounds = playingSounds + sound.id
                                        } else {
                                            android.util.Log.e("StarSkyScreen", "无法获取URI: ${sound.id}")
                                            Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("StarSkyScreen", "播放失败: ${e.message}")
                                    Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
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
                            }
                        }
                    }
                }
                } else {
                    // 其他分类，直接显示
                    val filteredSounds = remoteSounds.filter { it.category == category }
                    
                    // 使用 remember 配合分类 ID 作为 key，确保 tab 切换时保留滚动状态
                    // 直接从 Map 中获取或创建状态，由于 gridStates 在 remember 中保存，状态会在 tab 切换时保留
                    val lazyGridState = gridStates.getOrPut(category) { rememberLazyGridState() }
                    
                    // 监听滚动状态，触发浮动按钮收缩
                    LaunchedEffect(lazyGridState.isScrollInProgress) {
                        if (lazyGridState.isScrollInProgress) {
                            onScrollDetected()
                        }
                    }
                    
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                    items(filteredSounds.size) { index ->
                        val sound = filteredSounds[index]
                        RemoteSoundCard(
                            sound = sound,
                            displayName = getSoundDisplayName(sound),
                            isPlaying = playingSounds.contains(sound.id),
                            downloadProgress = downloadingSounds[sound.id],
                            isDownloadingButNoProgress = downloadingButNoProgress.contains(sound.id),
                            columnsCount = columnsCount,
                            isPinned = remotePinned.contains(sound.id),
                            isFavorite = remoteFavorites.contains(sound.id),
                            onPinnedChange = { isPinned ->
                                val newSet = remotePinned.toMutableSet()
                                if (isPinned) {
                                    // 检查音频是否已下载
                                    val cachedFile = cacheManager.getCachedFile(sound.id)
                                    if (cachedFile == null) {
                                        // 未下载，不允许置顶
                                        Toast.makeText(context, context.getString(R.string.must_download_before_pin), Toast.LENGTH_SHORT).show()
                                    } else {
                                        // 检查是否已达到最大数量（3个）
                                        if (newSet.size >= 3) {
                                            Toast.makeText(context, context.getString(R.string.max_3_sounds_limit), Toast.LENGTH_SHORT).show()
                                        } else {
                                            newSet.add(sound.id)
                                            remotePinned = newSet
                                            org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                        }
                                    }
                                } else {
                                    newSet.remove(sound.id)
                                    remotePinned = newSet
                                    org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                }
                            },
                            onFavoriteChange = { isFavorite ->
                                val newSet = remoteFavorites.toMutableSet()
                                if (isFavorite) {
                                    newSet.add(sound.id)
                                } else {
                                    newSet.remove(sound.id)
                                }
                                remoteFavorites = newSet
                                org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(context, newSet)
                            },
                            onCardClick = {
                                scope.launch {
                                    try {
                                        // 首先检查是否正在播放，如果正在播放则停止播放
                                        val currentlyPlaying = audioManager.isPlayingRemoteSound(sound.id)
                                        if (currentlyPlaying) {
                                            audioManager.pauseRemoteSound(sound.id)
                                            return@launch
                                        }
                                        
                                        // 如果未播放，检查是否需要下载
                                        val cachedFile = cacheManager.getCachedFile(sound.id)
                                        if (cachedFile == null && sound.remoteUrl != null) {
                                            // 开始下载，添加到"正在下载但还没有进度"集合
                                            downloadingButNoProgress = downloadingButNoProgress + sound.id
                                            // 开始下载
                                            val downloadFlow = cacheManager.downloadAudioWithProgress(
                                                sound.remoteUrl,
                                                sound.id
                                            )
                                            downloadFlow.collect { progress ->
                                                when (progress) {
                                                    is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                        val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                        android.util.Log.d("StarSkyScreen", "下载进度: ${sound.id} = $percent")
                                                        // 收到第一个进度更新，从"正在下载但还没有进度"集合中移除
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            put(sound.id, percent)
                                                        }
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                        android.util.Log.d("StarSkyScreen", "下载完成: ${sound.id}")
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            remove(sound.id)
                                                        }
                                                        // 下载完成后播放
                                                        val uri = resourceManager.getSoundUri(sound)
                                                        if (uri != null) {
                                                            audioManager.playRemoteSound(context, sound, uri)
                                                            playingSounds = playingSounds + sound.id
                                                        } else {
                                                            android.util.Log.e("StarSkyScreen", "下载完成后无法获取URI: ${sound.id}")
                                                            Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@collect
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        android.util.Log.e("StarSkyScreen", "下载失败: ${sound.id} - ${progress.exception.message}")
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            remove(sound.id)
                                                        }
                                                        Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                                                        return@collect
                                                    }
                                                }
                                            }
                                        } else {
                                            // 已缓存或直接播放
                                            val uri = resourceManager.getSoundUri(sound)
                                            if (uri != null) {
                                                audioManager.playRemoteSound(context, sound, uri)
                                                playingSounds = playingSounds + sound.id
                                            } else {
                                                android.util.Log.e("StarSkyScreen", "无法获取URI: ${sound.id}")
                                                Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("StarSkyScreen", "播放失败: ${e.message}")
                                        Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
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
        // 空状态
        else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Satellite,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
        Text(
            text = context.getString(R.string.star_sky_more_sounds_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
                }
            }
        }
        }
        
        // 音量调节弹窗
        if (showVolumeDialog && selectedSoundForVolume != null) {
            val sound = selectedSoundForVolume!!
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text(getSoundDisplayName(sound)) },
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
                        
                        // 音量滑块
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = volume,
                                onValueChange = { 
                                    volume = it
                                    // 实时应用到该声音
                                    audioManager.setRemoteVolume(sound.id, volume)
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
        
    }
}

/**
 * 网络音频卡片
 */
@Composable
fun RemoteSoundCard(
    sound: org.xmsleep.app.audio.model.SoundMetadata,
    displayName: String,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloadingButNoProgress: Boolean = false, // 正在下载但还没有收到进度
    columnsCount: Int = 2,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    onCardClick: () -> Unit,
    onVolumeClick: () -> Unit,
    cardHeight: androidx.compose.ui.unit.Dp? = null, // 自定义卡片高度，用于快捷播放模块
    isEditMode: Boolean = false, // 编辑模式，用于快捷播放模块
    onRemove: () -> Unit = {} // 删除回调，用于快捷播放模块编辑模式
) {
    val context = LocalContext.current
    val cacheManager = remember { 
        org.xmsleep.app.audio.AudioCacheManager.getInstance(context) 
    }
    var isCached by remember { mutableStateOf(cacheManager.getCachedFile(sound.id) != null) }
    
    // 监听下载完成，更新缓存状态
    LaunchedEffect(downloadProgress, sound.id, isPlaying) {
        // 当下载进度为1.0时（下载完成），延迟一点再检查缓存状态，确保文件已经写入
        if (downloadProgress != null && downloadProgress >= 1.0f) {
            delay(100) // 延迟100ms确保文件已经写入
            isCached = cacheManager.getCachedFile(sound.id) != null
        }
        // 当下载进度为null时（下载完成或未开始），检查缓存状态
        if (downloadProgress == null) {
            isCached = cacheManager.getCachedFile(sound.id) != null
        }
        // 当开始播放时，也检查缓存状态（因为可能从缓存播放）
        if (isPlaying) {
            isCached = cacheManager.getCachedFile(sound.id) != null
        }
    }
    
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    // 根据缓存状态设置不同的背景颜色和边框
    val cardBackgroundColor = if (isCached) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        // 未下载的卡片使用surfaceContainer作为背景，确保与背景有明显对比
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    
    // 菜单状态
    var showTitleMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 根据使用场景确定卡片高度：快捷播放模块使用80.dp，其他场景使用100.dp
    val finalCardHeight = cardHeight ?: if (columnsCount == 3) 80.dp else 100.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(finalCardHeight)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onCardClick() },
                            onLongPress = { 
                                scope.launch {
                                    showTitleMenu = true
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        border = if (!isCached) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 根据卡片高度调整内边距和文字样式
            val padding = if (finalCardHeight == 80.dp) 12.dp else 16.dp
            val textStyle = if (finalCardHeight == 80.dp) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
            val maxLines = if (finalCardHeight == 80.dp) 1 else 2
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 标题（左上角）
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    Text(
                        text = displayName,
                        style = textStyle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(alpha),
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 旋转加载动画（右上角，在下载开始但还没有收到进度时显示）
                if (isDownloadingButNoProgress && (downloadProgress == null || downloadProgress == 0f)) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 菜单弹窗（复刻首页样式，编辑模式下不显示）
                // 将菜单移到外层 Box，确保能正确显示
                if (!isEditMode) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                        // 置顶选项
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isPinned) context.getString(R.string.cancel_default) else context.getString(R.string.set_as_default),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                onPinnedChange(!isPinned)
                                showTitleMenu = false
                            }
                        )
                        
                        // 收藏选项
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isFavorite) context.getString(R.string.cancel_favorite) else context.getString(R.string.favorite),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                onFavoriteChange(!isFavorite)
                                showTitleMenu = false
                            }
                        )
                        
                        // 删除缓存选项（仅当已缓存时显示）
                        if (isCached) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = context.getString(R.string.delete_cache),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    scope.launch {
                                        try {
                                            cacheManager.deleteCache(sound.id)
                                            val cachedFile = cacheManager.getCachedFile(sound.id)
                                            if (cachedFile == null) {
                                                isCached = false
                                                Toast.makeText(context, context.getString(R.string.cache_cleared_success), Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.cache_clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                        }
                                        showTitleMenu = false
                                    }
                                }
                            )
                        }
                        }
                    }
                }
                
                // 音频可视化器（左下角，只在播放时显示）
                if (isPlaying) {
                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(24.dp, 16.dp)
                            .alpha(alpha),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 删除按钮（右下角，只在编辑模式下显示，快捷播放模块）
                if (isEditMode) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = 8.dp) // 向下偏移8dp，距离底部20dp
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = context.getString(R.string.remove),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // 音量图标（右下角，只在播放时显示，快捷播放模块不显示，编辑模式下也不显示）
                // 使用 cardHeight 参数判断是否是快捷播放模块，而不是 finalCardHeight
                if (isPlaying && cardHeight == null && !isEditMode) {
                    IconButton(
                        onClick = onVolumeClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 12.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = context.getString(R.string.adjust_volume),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // 下载进度条（卡片底部，宽度跟卡片一样）
            if (downloadProgress != null && downloadProgress > 0f) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

