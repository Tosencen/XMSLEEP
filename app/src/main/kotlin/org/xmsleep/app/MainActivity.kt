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
import androidx.core.content.ContextCompat
import com.materialkolor.hct.Hct
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.theme.XMSLEEPTheme
import org.xmsleep.app.ui.MainScreen
import org.xmsleep.app.ui.CrashScreen
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.crash.CrashHandler
import org.xmsleep.app.crash.getCrashInfo

/**
 * XMSLEEP 主Activity
 * 负责应用启动、权限请求和主题配置
 */
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageManager.updateAppLanguage(it) })
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化全局异常处理器
        CrashHandler.init(this)
        
        // 在应用启动时迁移旧版本的数据（如果存在）
        org.xmsleep.app.preferences.PreferencesManager.migrateFromOldVersion(this)
        
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
    
    // 请求存储权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限请求结果处理
        Logger.d("MainActivity", "存储权限请求结果: $permissions")
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
        
        // Android 13+ 需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    // 调色板颜色列表（30° 间隔均匀分布的柔和粉彩色，12色）
    val paletteColors = remember {
        listOf(
            Color(Hct.from(0.0, 45.0, 75.0).toInt()),    // 1. 柔和红
            Color(Hct.from(30.0, 45.0, 75.0).toInt()),   // 2. 柔和橙
            Color(Hct.from(60.0, 45.0, 75.0).toInt()),   // 3. 柔和黄
            Color(Hct.from(90.0, 45.0, 75.0).toInt()),   // 4. 柔和黄绿
            Color(Hct.from(120.0, 45.0, 75.0).toInt()),  // 5. 柔和绿
            Color(Hct.from(150.0, 45.0, 75.0).toInt()),  // 6. 柔和青绿
            Color(Hct.from(180.0, 45.0, 75.0).toInt()),  // 7. 柔和青
            Color(Hct.from(210.0, 45.0, 75.0).toInt()),  // 8. 柔和蓝
            Color(Hct.from(240.0, 45.0, 75.0).toInt()),  // 9. 柔和靛蓝
            Color(Hct.from(270.0, 45.0, 75.0).toInt()),  // 10. 柔和紫色（修复：改为270°，与第11个拉开差距）
            Color(Hct.from(300.0, 45.0, 75.0).toInt()),  // 11. 柔和品红
            Color(Hct.from(330.0, 45.0, 75.0).toInt()),  // 12. 柔和粉红
        )
    }
    
    // 主题状态管理（从SharedPreferences加载保存的设置）
    var darkMode by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getDarkMode(context))
    }
    var selectedColor by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getSelectedColor(context, paletteColors[3]))
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
    
    // 应用主题
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
                    onDarkModeChange = { newMode ->
                        darkMode = newMode
                        org.xmsleep.app.preferences.PreferencesManager.saveDarkMode(context, newMode)
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
