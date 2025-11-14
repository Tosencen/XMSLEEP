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
import org.xmsleep.app.utils.Logger

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
        
        // 在应用启动时迁移旧版本的数据（如果存在）
        org.xmsleep.app.preferences.PreferencesManager.migrateFromOldVersion(this)
        
        setContent {
            XMSLEEPApp()
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
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    // 调色板颜色列表
    val paletteColors = remember {
        listOf(
            Color(Hct.from(140.0, 40.0, 40.0).toInt()),
            Color(Hct.from(175.0, 40.0, 40.0).toInt()),
            Color(Hct.from(210.0, 40.0, 40.0).toInt()),
            Color(Hct.from(245.0, 40.0, 40.0).toInt()),
            Color(Hct.from(280.0, 40.0, 40.0).toInt()),
            Color(0xFF4F378B),  // 默认紫色
            Color(Hct.from(315.0, 40.0, 40.0).toInt()),
            Color(Hct.from(350.0, 40.0, 40.0).toInt()),
            Color(Hct.from(35.0, 40.0, 40.0).toInt()),
            Color(Hct.from(70.0, 40.0, 40.0).toInt()),
            Color(Hct.from(105.0, 40.0, 40.0).toInt()),
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
