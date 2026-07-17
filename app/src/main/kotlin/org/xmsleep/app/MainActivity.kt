package org.xmsleep.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.theme.XMSLEEPTheme
import org.xmsleep.app.ui.MainScreen
import org.xmsleep.app.ui.BackgroundSelection
import org.xmsleep.app.ui.CrashScreen
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.ThemeColorExtractor
import org.xmsleep.app.crash.CrashHandler
import org.xmsleep.app.crash.getCrashInfo
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.ui.settings.copyToPrivateStorage
import org.xmsleep.app.ui.viewmodel.MainViewModel
import org.xmsleep.app.ui.viewmodel.SoundsViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * XMSLEEP 主Activity
 * 负责应用启动、权限请求和主题配置
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageManager.updateAppLanguage(it) })
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装 Splash Screen（必须在 super.onCreate 之前）
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 初始化日志框架
        Logger.init()
        
        // 初始化全局异常处理器
        CrashHandler.init(this)
        
        // 初始化本地音频媒体服务
        org.xmsleep.app.audio.LocalAudioMediaService.getInstance(this).initialize(this)
        
        // 在应用启动时迁移旧版本的数据（如果存在）
        org.xmsleep.app.preferences.PreferencesManager.migrateFromOldVersion(this)

        // 确保匿名设备标识已生成（共建/赞助身份，首次启动生成并持久化）
        org.xmsleep.app.preferences.PreferencesManager.getAnonymousDeviceId(this)
        
        // 初始化默认的音频清单（从 assets 加载到缓存）
        org.xmsleep.app.audio.AudioResourceManager.getInstance(this).initializeDefaultManifest()
        
        // 检查是否有崩溃信息
        val (errorMessage, stackTrace) = intent.getCrashInfo()
        
        setContent {
            if (errorMessage != null && stackTrace != null) {
                // 显示崩溃页面
                CrashScreen(
                    errorMessage = errorMessage,
                    stackTrace = stackTrace,
                    onRestart = {
                        // 清除崩溃信息并重新创建 Activity
                        intent.removeExtra("crash_error_message")
                        intent.removeExtra("crash_stack_trace")
                        recreate()
                    }
                )
            } else {
                // 正常显示应用
                XMSLEEPApp()
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        // 应用进入后台时保存最近播放记录
        try {
            val audioManager = org.xmsleep.app.audio.AudioManager.getInstance()
            audioManager.saveRecentPlayingSounds()
        } catch (e: Exception) {
            Logger.e("MainActivity", "保存最近播放记录失败: ${e.message}")
        }
    }
}

/**
 * XMSLEEP 应用入口Composable
 * 负责主题配置、语言管理和权限请求
 */
@Composable
fun XMSLEEPApp() {
    val context = LocalContext.current
    
    // 语言状态管理
    var currentLanguage by remember { mutableStateOf(LanguageManager.getCurrentLanguage(context)) }
    val localizedContext = remember(currentLanguage) {
        LanguageManager.createLocalizedContext(context, currentLanguage)
    }
    val localizedConfiguration = remember(currentLanguage) {
        localizedContext.resources.configuration
    }
    
    // 请求通知权限（Android 13+）
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Logger.d("MainActivity", "通知权限请求结果: $isGranted")
    }
    
    LaunchedEffect(Unit) {
        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    // 音频文件权限请求 launcher（用于本地音频访问）
    var shouldNavigateToLocalAudio by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Logger.d("MainActivity", "音频权限请求结果: $isGranted")
        if (isGranted && shouldNavigateToLocalAudio) {
            // 权限已授予，标记需要导航（MainScreen 会处理导航）
            shouldNavigateToLocalAudio = false
        } else if (!isGranted) {
            // 权限被拒绝，重置标记
            shouldNavigateToLocalAudio = false
        }
    }
    
    // 位置权限请求 launcher（用于天气功能）
    var onLocationPermissionResult: ((Boolean) -> Unit)? = null
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Logger.d("MainActivity", "位置权限请求结果: $isGranted")
        onLocationPermissionResult?.invoke(isGranted)
    }

    // 调色板颜色列表（硬编码的柔和粉彩色，12色）
    val paletteColors = remember {
        listOf(
            Color(0xFFE8B4B8), // 1. 柔和红
            Color(0xFFE8C9B4), // 2. 柔和橙
            Color(0xFFE8E0B4), // 3. 柔和黄
            Color(0xFFD4E8B4), // 4. 柔和黄绿
            Color(0xFFB4E8B4), // 5. 柔和绿
            Color(0xFFB4E8D4), // 6. 柔和青绿
            Color(0xFFB4E0E8), // 7. 柔和青
            Color(0xFFB4C9E8), // 8. 柔和蓝
            Color(0xFFB4B4E8), // 9. 柔和靛蓝
            Color(0xFFD4B4E8), // 10. 柔和紫色
            Color(0xFFE8B4E0), // 11. 柔和品红
            Color(0xFFE8B4C9), // 12. 柔和粉红
        )
    }
    
    // 主题状态管理（从SharedPreferences加载保存的设置）
    var darkMode by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getDarkMode(context))
    }
    
    // 背景动画的主题色（从缩略图同步提取）
    val backgroundThemeColors = remember {
        val colors = mutableMapOf<BackgroundSelection, Color>()
        val extractor = ThemeColorExtractor(context)
        
        Logger.d("MainActivity", "=== 开始提取背景主题色 ===")
        
        BackgroundSelection.entries.forEach { bg ->
            if (bg != BackgroundSelection.None) {
                val color = if (bg.themeColor != null) {
                    bg.themeColor
                } else if (bg.thumbnailResourceId != null) {
                    Logger.d("MainActivity", "尝试从缩略图提取: ${bg.thumbnailResourceId}")
                    val extracted = extractor.extractDominantColorSync(bg.thumbnailResourceId!!)
                    Logger.d("MainActivity", "提取结果: $extracted")
                    extracted
                } else {
                    null
                }
                
                if (color != null) {
                    colors[bg] = color
                    val colorValue = color.value
                    val colorHex = colorValue.toString(16).padStart(16, '0').substring(8).uppercase()
                    Logger.d("MainActivity", "✓ 背景 ${bg.name}: hex=#$colorHex")
                } else {
                    Logger.d("MainActivity", "✗ 背景 ${bg.name}: 提取失败")
                }
            }
        }
        
        Logger.d("MainActivity", "=== 背景主题色提取完成，共提取 ${colors.size} 个颜色 ===")
        colors.toMap()
    }
    
    var backgroundSelection by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getBackgroundSelection(context))
    }

    var customBackgroundUri by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getCustomBackgroundUri(context))
    }

    var customBackgroundColor by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getCustomBackgroundColor(context, Color.Unspecified))
    }

    var customBackgroundThumbnail by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getCustomBackgroundThumbnail(context))
    }

    // 对话框中待提交的自定义背景（选择文件后暂存，点确定才提交）
    var pendingCustomBgUri by remember { mutableStateOf<String?>(null) }
    var pendingCustomBgThumbnail by remember { mutableStateOf<String?>(null) }
    var pendingCustomBgColor by remember { mutableStateOf<Color?>(null) }

    // 自定义背景文件选择 launcher
    val coroutineScope = rememberCoroutineScope()
    val customBackgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val (copiedUri, copiedThumbUri) = withContext(Dispatchers.IO) {
                        copyToPrivateStorage(context, uri)
                    }
                    if (copiedUri != null) {
                        val fileUri = copiedUri
                        val thumbUri = copiedThumbUri
                        val color = withContext(Dispatchers.IO) {
                            try {
                                ThemeColorExtractor(context).extractDominantColorFromUri(fileUri)
                            } catch (_: Exception) { null }
                        }
                        pendingCustomBgUri = fileUri.toString()
                        pendingCustomBgThumbnail = thumbUri?.toString()
                        pendingCustomBgColor = color ?: org.xmsleep.app.preferences.PreferencesManager.getSelectedColor(context, paletteColors[3])
                    }
                } catch (e: Exception) {
                    Logger.e("MainActivity", "自定义背景文件处理失败", e)
                }
            }
        }
    }

    val savedColor = org.xmsleep.app.preferences.PreferencesManager.getSelectedColor(context, paletteColors[3])

    // 如果有背景选择，使用背景主题色；否则从 SharedPreferences 加载保存的调色板颜色
    var selectedColor by remember {
        mutableStateOf(savedColor)
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
    
    var backgroundOpacity by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getBackgroundOpacity(context))
    }
    var backgroundBlurRadius by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getBackgroundBlurRadius(context))
    }

    // 确定提交自定义背景（用户点击对话框"确定"时调用）
    val onCommitCustomBg: () -> Unit = {
        pendingCustomBgUri?.let { uri ->
            customBackgroundUri = uri
            org.xmsleep.app.preferences.PreferencesManager.saveCustomBackgroundUri(context, uri)
            pendingCustomBgThumbnail?.let { thumb ->
                customBackgroundThumbnail = thumb
                org.xmsleep.app.preferences.PreferencesManager.saveCustomBackgroundThumbnail(context, thumb)
            }
            pendingCustomBgColor?.let { color ->
                    customBackgroundColor = color
                    org.xmsleep.app.preferences.PreferencesManager.saveCustomBackgroundColor(context, color)
                }
            backgroundSelection = BackgroundSelection.Custom
            org.xmsleep.app.preferences.PreferencesManager.saveBackgroundSelection(context, BackgroundSelection.Custom)
        }
        pendingCustomBgUri = null
        pendingCustomBgThumbnail = null
        pendingCustomBgColor = null
    }

    // 取消自定义背景（用户取消对话框时调用）
    val onCancelCustomBg: () -> Unit = {
        pendingCustomBgUri?.let { uri ->
            try {
                java.io.File(java.net.URI.create(uri)).delete()
            } catch (_: Exception) {}
        }
        // 不改变 backgroundSelection，保持用户打开弹窗前的状态
        pendingCustomBgUri = null
        pendingCustomBgThumbnail = null
        pendingCustomBgColor = null
    }

    // 自定义背景下选择主题色（不切换到无背景）
    val onCustomColorChange: (Color) -> Unit = { color ->
        selectedColor = color
        org.xmsleep.app.preferences.PreferencesManager.saveSelectedColor(context, color)
    }

    // 计算是否使用深色主题
    val isDark = when (darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> isSystemInDarkTheme()
    }
    
    // 应用主题
    XMSLEEPTheme(
        isDark = isDark,
        seedColor = selectedColor,
        useDynamicColor = useDynamicColor,
        useBlackBackground = useBlackBackground
    ) {
        // 在 CompositionLocalProvider 之前创建 ViewModel，保留 Activity context
        val mainViewModel: MainViewModel = hiltViewModel()
        val soundsViewModel: SoundsViewModel = hiltViewModel()
        
        // 使用CompositionLocalProvider提供语言化的Context和Configuration
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration
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
                    backgroundSelection = backgroundSelection,
                    soundCardsColumnsCount = soundCardsColumnsCount,
                    currentLanguage = currentLanguage,
                    audioPermissionLauncher = audioPermissionLauncher,
                    locationPermissionLauncher = locationPermissionLauncher,
                    onAudioPermissionGranted = { shouldNavigateToLocalAudio = true },
                    onLanguageChange = { currentLanguage = it },
                    onDarkModeChange = { newMode ->
                        darkMode = newMode
                        org.xmsleep.app.preferences.PreferencesManager.saveDarkMode(context, newMode)
                    },
                    onColorChange = { 
                        val colorHex = it.value.toString(16).padStart(8, '0').takeLast(6).uppercase()
                        Logger.d("MainActivity", "用户选择调色板颜色: #$colorHex")
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
                    onBackgroundSelectionChange = { newBackground ->
                        Logger.d("MainActivity", "背景切换: $newBackground")

                        backgroundSelection = newBackground
                        org.xmsleep.app.preferences.PreferencesManager.saveBackgroundSelection(context, newBackground)
                    },
                    onSoundCardsColumnsCountChange = {
                        soundCardsColumnsCount = it
                        org.xmsleep.app.preferences.PreferencesManager.saveSoundCardsColumnsCount(context, it)
                    },
                    backgroundOpacity = backgroundOpacity,
                    backgroundBlurRadius = backgroundBlurRadius,
                    onBackgroundOpacityChange = {
                        backgroundOpacity = it
                        org.xmsleep.app.preferences.PreferencesManager.saveBackgroundOpacity(context, it)
                    },
                    onBackgroundBlurRadiusChange = {
                        backgroundBlurRadius = it
                        org.xmsleep.app.preferences.PreferencesManager.saveBackgroundBlurRadius(context, it)
                    },
                    paletteColors = paletteColors,
                    mainViewModel = mainViewModel,
                    soundsViewModel = soundsViewModel,
                    customBackgroundUri = customBackgroundUri,
                    customBackgroundColor = customBackgroundColor,
                    customBackgroundThumbnail = customBackgroundThumbnail,
                    pendingCustomBgUri = pendingCustomBgUri,
                    pendingCustomBgThumbnail = pendingCustomBgThumbnail,
                    pendingCustomBgColor = pendingCustomBgColor,
                    onPickCustomBackground = { customBackgroundLauncher.launch(arrayOf("image/*", "video/mp4")) },
                    onCommitCustomBg = onCommitCustomBg,
                    onCancelCustomBg = onCancelCustomBg,
                    onCustomColorChange = onCustomColorChange,
                )
            }
        }
    }
}