package org.xmsleep.app.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs
import java.util.concurrent.TimeUnit
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.update.UpdateViewModel
import org.xmsleep.app.update.UpdateState
import org.xmsleep.app.update.UpdateDialog
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import com.airbnb.lottie.value.LottieFrameInfo
import org.xmsleep.app.utils.ToastUtils
import org.xmsleep.app.weather.WeatherData
import org.xmsleep.app.weather.WeatherService
import org.xmsleep.app.weather.WeatherSoundMapper
import org.xmsleep.app.utils.Logger

/**
 * 自定义颜色回调，根据原颜色的亮度和饱和度动态映射到主题色系或灰色系
 */
internal class ThemeColorCallback(
    private val darkColor: Int,
    private val mediumColor: Int,
    private val lightColor: Int,
    private val secondaryColor: Int,
    private val backgroundColor: Int,
    private val darkGrayColor: Int,
    private val mediumGrayColor: Int,
    private val lightGrayColor: Int,
    private val primaryHct: Hct,
    private val isDarkMode: Boolean
) : LottieValueCallback<Int>() {
    override fun getValue(frameInfo: LottieFrameInfo<Int>): Int {
        // LottieFrameInfo的value属性可能为null，使用startValue或endValue
        val originalColor = frameInfo.startValue ?: frameInfo.endValue ?: return mediumColor
        
        // 计算原颜色的亮度（使用相对亮度公式）
        val originalArgb = originalColor
        val r = (originalArgb ushr 16) and 0xFF
        val g = (originalArgb ushr 8) and 0xFF
        val b = originalArgb and 0xFF
        
        // 计算相对亮度（0-1之间的值）
        val luminance = (0.299 * r.toDouble() + 0.587 * g.toDouble() + 0.114 * b.toDouble()) / 255.0
        
        // 计算颜色的饱和度（用于识别背景）
        val max = maxOf(r, g, b).toDouble()
        val min = minOf(r, g, b).toDouble()
        val saturation = if (max == 0.0) 0.0 else (max - min) / max
        
        // 识别灰色系颜色（低饱和度但不是极端亮/暗的颜色）
        // 灰色系应该保持为灰色，而不是映射为彩色主题色
        val isGrayColor = saturation < 0.15 && luminance > 0.15 && luminance < 0.85
        
        // 如果是灰色系，使用对应的灰色
        if (isGrayColor) {
            // 根据原始亮度映射到灰色系
            return when {
                luminance < 0.35 -> darkGrayColor
                luminance < 0.65 -> mediumGrayColor
                else -> lightGrayColor
            }
        }
        
        // 识别背景元素：
        // 背景blob通常是浅色、低饱和度的填充色（没有描边的区域）
        // 浅色模式：高亮度（>0.7）、低饱和度（<0.5）
        // 深色模式：也要识别原本高亮度的背景（>0.6），通过饱和度和颜色特征判断
        val isLikelyBackground = if (isDarkMode) {
            // 深色模式：识别原本是浅色的背景blob
            // 高亮度（>0.6）且低饱和度（<0.5），这是典型的浅色背景特征
            // 即使原动画是浅色，在深色模式下也要映射为较暗的主题色背景
            (luminance > 0.6 && saturation >= 0.15 && saturation < 0.5) || 
            // 或者中等亮度但饱和度较低（可能是背景）
            (luminance >= 0.4 && saturation >= 0.15 && saturation < 0.3)
        } else {
            // 浅色模式：背景通常是高亮度、低饱和度的填充色
            luminance > 0.7 && saturation >= 0.15 && saturation < 0.5
        }
        
        // 如果识别为背景，使用对应的主题色
        if (isLikelyBackground) {
            return backgroundColor
        }
        
        // 根据亮度映射到不同的主题色深浅版本，形成清晰的分层
        // 深色模式下，人物的主要部分（帽子、衣服、头发）应该使用相对一致的中间色
        // 避免过亮导致的米黄色/beige感
        return when {
            // 非常暗的部分（亮度 < 0.25）- 使用深色（用于鞋子、阴影等）
            luminance < 0.25 -> darkColor
            // 暗到中等亮度（0.25 - 0.55）- 使用中间色（人物主要部分）
            // 这样可以让人物的帽子、衣服、头发颜色更协调
            luminance < 0.55 -> {
                // 如果原颜色比较接近主色调（通过色相判断），使用中间色
                // 否则使用secondary色作为变化
                try {
                    val originalHct = Color(originalArgb).toHct()
                    val hueDiff = abs(originalHct.hue - primaryHct.hue)
                    if (hueDiff < 30 || hueDiff > 330) {
                        // 色相接近主色，使用中间色（人物主要部分）
                        mediumColor
                    } else {
                        // 色相差异较大，使用secondary色
                        secondaryColor
                    }
                } catch (e: Exception) {
                    // 如果转换失败，使用中间色
                    mediumColor
                }
            }
            // 较亮的中间亮度（0.55 - 0.75）- 仍然主要使用中间色，保持人物颜色协调
            luminance < 0.75 -> {
                // 对于原本较亮的颜色，在深色模式下稍微提亮，但不要太亮
                if (isDarkMode) {
                    mediumColor // 深色模式下保持中等亮度，避免过亮
                } else {
                    lightColor // 浅色模式下可以使用较亮的颜色
                }
            }
            // 非常亮的部分（亮度 >= 0.75）- 使用浅色（用于高光、描边等）
            else -> lightColor
        }
    }
}

/**
 * 声音模块数据类
 */
data class SoundItem(
    val sound: AudioManager.Sound,
    val name: String,
    val animationRes: Int,
    val color: Color
)

/**
 * 声音播放界面
 *
 * TODO(重构): 此文件已超过 3600 行，建议拆分为以下独立文件：
 *  - SoundsScreen.kt        —— 主屏幕入口 Composable（~300行）
 *  - SoundCard.kt           —— SoundCard 及动画相关组件（~500行，当前第2196~2645行）
 *  - SoundDialogs.kt        —— VolumeDialog / TimerDialog（~300行，当前第2994~3240行）
 *  - SoundComponents.kt     —— AudioVisualizer / EmptyStateAnimation / CustomChevronIcon 等（~400行）
 *  - SoundsScreenContent.kt —— BuiltInSoundsContent / FavoriteSoundsContent / OnlineSoundsContent（~300行）
 * 拆分时注意 private 函数的可见性迁移。
 */

/**
 * 获取位置信息（优先缓存位置，若无则主动请求一次定位）
 * @return Location 或 null
 */
@Suppress("MissingPermission")
private fun obtainLocation(context: android.content.Context): android.location.Location? {
    return try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager

        // 优先使用缓存的位置
        val cached = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        if (cached != null) return cached

        // 缓存为空时，尝试主动请求一次定位
        // 注意：requestSingleUpdate 是阻塞调用，不应在主线程执行
        var result: android.location.Location? = null
        if (locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            try {
                locationManager.requestSingleUpdate(
                    android.location.LocationManager.NETWORK_PROVIDER,
                    object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) { result = location }
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    },
                    android.os.Looper.getMainLooper()
                )
                // 等待定位结果，最多3秒
                var waited = 0
                while (result == null && waited < 3000) {
                    Thread.sleep(100)
                    waited += 100
                }
            } catch (e: Exception) {
                Logger.w("WeatherHelper", "主动定位失败: ${e.message}")
            }
        }
        result
    } catch (e: Exception) {
        Logger.e("WeatherHelper", "获取位置失败: ${e.message}")
        null
    }
}

/**
 * 获取并刷新天气数据
 * @return 刷新成功返回 WeatherData，失败返回 null
 */
private suspend fun fetchAndRefreshWeather(context: android.content.Context): WeatherData? {
    // 检查位置权限
    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!hasLocationPermission) return null

    return try {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val location = obtainLocation(context) ?: return@withContext null

            val weatherService = WeatherService()
            val result = weatherService.getWeather(location.latitude, location.longitude)
            result.onSuccess { data ->
                WeatherSoundMapper.saveLastWeather(
                    context, data.weatherCode,
                    location.latitude, location.longitude,
                    data.temperature, data.cityName,
                    data.humidity, data.feelsLike
                )
            }.getOrNull()
        }
    } catch (e: Exception) {
        Logger.e("SoundsScreen", "获取天气失败: ${e.message}")
        null
    }
}
@Composable
fun SoundsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = false,
    backgroundSelection: BackgroundSelection = BackgroundSelection.None,
    onBackgroundSelectionChange: (BackgroundSelection) -> Unit = {},
    columnsCount: Int = 2,
    onColumnsCountChange: (Int) -> Unit = {},
    preset1Sounds: MutableState<MutableSet<AudioManager.Sound>>,
    preset2Sounds: MutableState<MutableSet<AudioManager.Sound>>,
    preset3Sounds: MutableState<MutableSet<AudioManager.Sound>>,
    activePreset: Int = 1,
    onActivePresetChange: (Int) -> Unit = {},
    hasAnyPresetItems: Boolean = false,
    onNavigateToFlipClock: () -> Unit = {},
    onScrollDetected: () -> Unit = {},
    onQuickPlayExpand: () -> Unit = {},
    updateViewModel: UpdateViewModel? = null,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    contentAlpha: Float = 1f // 内容透明度（用于禁用点击）
) {
    // 预设弹窗显示状态
    var showPresetDialog by remember { mutableStateOf(false) }
    // 根据 activePreset 动态获取当前预设的 pinnedSounds
    val pinnedSounds = when (activePreset) {
        1 -> preset1Sounds
        2 -> preset2Sounds
        3 -> preset3Sounds
        else -> preset1Sounds
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val audioManager = remember { AudioManager.getInstance() }
    val timerManager = remember { TimerManager.getInstance() }
    val resourceManager = remember { org.xmsleep.app.audio.AudioResourceManager.getInstance(context) }
    val cacheManager = remember { org.xmsleep.app.audio.AudioCacheManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    // 更新相关状态
    val updateState by (updateViewModel?.updateState ?: MutableStateFlow(UpdateState.Idle)).collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    
    // 判断是否显示更新图标（有新版本或已下载但未安装）
    val showUpdateIcon = remember(updateState) {
        updateState is UpdateState.HasUpdate || updateState is UpdateState.Downloaded
    }
    
    // 各声音的播放状态
    val playingStates = remember { mutableStateMapOf<AudioManager.Sound, Boolean>() }
    
    // 追踪每个声音是从哪个预设播放的（用于在预设弹窗中只显示当前预设的播放状态）
    val soundPlayingPreset = remember { mutableStateMapOf<AudioManager.Sound, Int>() }
    
    // 远程音频相关状态
    var remoteSounds by remember { mutableStateOf<List<org.xmsleep.app.audio.model.SoundMetadata>>(emptyList()) }
    var remotePinned by remember(activePreset) { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, activePreset).toMutableSet()) 
    }
    
    // 监听 activePreset 变化，重新加载对应的远程音频固定状态
    LaunchedEffect(activePreset) {
        val newPinned = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, activePreset).toMutableSet()
        remotePinned = newPinned
        Logger.d("SoundsScreen", "切换到预设 $activePreset，远程固定音频数量: ${newPinned.size}")
    }
    var downloadingSounds by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var playingRemoteSounds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 获取当前语言
    val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
    
    // 获取音频显示名称的辅助函数
    fun getSoundDisplayName(sound: org.xmsleep.app.audio.model.SoundMetadata): String {
        return sound.getLocalizedName(currentLanguage)
    }
    
    // 加载远程音频列表
    LaunchedEffect(Unit) {
        try {
            val sounds = resourceManager.getRemoteSounds()
            remoteSounds = sounds
        } catch (e: Exception) {
            Logger.e("SoundsScreen", "加载远程音频失败: ${e.message}")
        }
    }
    
    // 天气智能推荐状态
    var weatherEnabled by remember { mutableStateOf(WeatherSoundMapper.isEnabled(context)) }
    var currentWeather by remember { mutableStateOf<WeatherData?>(null) }
    
    // 定期检查天气开关状态（用户可能在设置页面更改）
    LaunchedEffect(Unit) {
        while (true) {
            val enabled = WeatherSoundMapper.isEnabled(context)
            if (enabled != weatherEnabled) {
                weatherEnabled = enabled
            }
            delay(1000)
        }
    }
    
    // 天气数据获取（首次加载）
    LaunchedEffect(weatherEnabled) {
        if (!weatherEnabled) {
            currentWeather = null
            return@LaunchedEffect
        }

        // 先尝试加载上次缓存的天气（立即显示）
        val lastWeather = WeatherSoundMapper.getLastWeather(context)
        if (lastWeather != null) {
            currentWeather = lastWeather
        }

        // 如果缓存有效，先不刷新（等定期刷新）
        if (WeatherSoundMapper.isWeatherCacheValid(context)) return@LaunchedEffect

        // 缓存过期，立即刷新
        fetchAndRefreshWeather(context)?.let { currentWeather = it }
    }

    // 定期刷新天气数据（每10分钟），带错误退避
    LaunchedEffect(weatherEnabled) {
        if (!weatherEnabled) return@LaunchedEffect
        var consecutiveFailures = 0
        while (true) {
            // 错误退避：连续失败时逐步增加间隔（最多30分钟）
            val baseInterval = 10 * 60 * 1000L // 10分钟
            val backoff = minOf(consecutiveFailures * 5 * 60 * 1000L, 30 * 60 * 1000L)
            delay(baseInterval + backoff)

            val result = fetchAndRefreshWeather(context)
            if (result != null) {
                currentWeather = result
                consecutiveFailures = 0
            } else {
                consecutiveFailures++
            }
        }
    }
    
    // 监听远程音频置顶状态变化（从 PreferencesManager 读取）
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // 每500ms检查一次
            val savedPinned = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, activePreset).toMutableSet()
            // 比较内容是否相同（使用 toSet() 进行比较）
            if (savedPinned.toSet() != remotePinned.toSet()) {
                remotePinned = savedPinned
            }
        }
    }
    
    // 监听远程音频播放状态
    LaunchedEffect(Unit) {
        while (true) {
            delay(100) // 缩短到100ms，提高远程音频状态响应速度
            val currentlyPlaying = remoteSounds.filter { sound ->
                remotePinned.contains(sound.id) && audioManager.isPlayingRemoteSound(sound.id)
            }.map { it.id }.toSet()
            playingRemoteSounds = currentlyPlaying
        }
    }
    
    // 倒计时相关状态
    val isTimerActive by timerManager.isTimerActive.collectAsState()
    val timeLeftMillis by timerManager.timeLeftMillis.collectAsState()
    var showTimerDialog by remember { mutableStateOf(false) }
    
    // 检查是否有声音在播放
    val hasPlayingSound = remember {
        derivedStateOf {
            playingStates.values.any { it }
        }
    }.value
    
    // 14个声音模块的数据（使用字符串资源以支持语言切换）
    // 使用 configuration.locales 作为依赖，确保语言切换时重新创建
    val soundItems = remember(colorScheme, configuration.locales) {
        listOf(
            // 1. 早晨咖啡
            SoundItem(
                AudioManager.Sound.MORNING_COFFEE,
                context.getString(R.string.sound_morning_coffee),
                R.drawable.morning_coffee,
                colorScheme.primaryContainer
            ),
            // 2. 风车
            SoundItem(
                AudioManager.Sound.WINDMILL,
                context.getString(R.string.sound_windmill),
                R.drawable.windmill,
                colorScheme.tertiaryContainer
            ),
            // 3. 伞上雨声
            SoundItem(
                AudioManager.Sound.UMBRELLA_RAIN,
                context.getString(R.string.sound_umbrella_rain),
                R.drawable.umbrella_rain,
                colorScheme.primary
            ),
            // 4. 打字机
            SoundItem(
                AudioManager.Sound.TYPEWRITER,
                context.getString(R.string.sound_typewriter),
                R.drawable.typewriter,
                colorScheme.secondaryContainer
            ),
            // 5. 时钟
            SoundItem(
                AudioManager.Sound.CLOCK,
                context.getString(R.string.sound_clock),
                R.drawable.clock,
                colorScheme.errorContainer
            ),
            // 6. 划船
            SoundItem(
                AudioManager.Sound.ROWING,
                context.getString(R.string.sound_rowing),
                R.drawable.rowing,
                colorScheme.error
            ),
            // 7. 森林鸟鸣
            SoundItem(
                AudioManager.Sound.FOREST_BIRDS,
                context.getString(R.string.sound_forest_birds),
                R.drawable.forest_birds,
                colorScheme.primary
            ),
            // 8. 漂流
            SoundItem(
                AudioManager.Sound.DRIFTING,
                context.getString(R.string.sound_drifting),
                R.drawable.drifting,
                colorScheme.error
            ),
            // 9. 打雷
            SoundItem(
                AudioManager.Sound.THUNDER,
                context.getString(R.string.sound_thunder),
                R.drawable.thunder,
                colorScheme.tertiaryContainer
            ),
            // 10. 篝火
            SoundItem(
                AudioManager.Sound.CAMPFIRE,
                context.getString(R.string.sound_campfire),
                R.drawable.campfire,
                colorScheme.secondary
            ),
            // 11. 雪地徒步
            SoundItem(
                AudioManager.Sound.SNOW_WALKING,
                context.getString(R.string.sound_snow_walking),
                R.drawable.snow_walking,
                colorScheme.secondaryContainer
            ),
            // 12. 起风了
            SoundItem(
                AudioManager.Sound.WIND,
                context.getString(R.string.sound_wind),
                R.drawable.wind,
                colorScheme.tertiary
            ),
            // 13. 大雨
            SoundItem(
                AudioManager.Sound.HEAVY_RAIN,
                context.getString(R.string.sound_heavy_rain),
                R.drawable.heavy_rain,
                colorScheme.primaryContainer
            ),
            // 14. 图书馆
            SoundItem(
                AudioManager.Sound.LIBRARY,
                context.getString(R.string.sound_library),
                R.drawable.library,
                colorScheme.tertiary
            ),
            // 15. 键盘
            SoundItem(
                AudioManager.Sound.KEYBOARD,
                context.getString(R.string.sound_keyboard),
                R.drawable.keyboard,
                colorScheme.primaryContainer
            ),
            // 16. 办公室
            SoundItem(
                AudioManager.Sound.OFFICE,
                context.getString(R.string.sound_office),
                R.drawable.office,
                colorScheme.secondary
            )
        )
    }
    
    // 初始化状态（只读取状态，不停止播放）
    LaunchedEffect(Unit) {
        // 初始化播放状态为实际播放状态
        soundItems.forEach { item ->
            playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
        }
    }
    
    // 定期更新播放状态（立即启动，确保状态同步）
    LaunchedEffect(Unit) {
        while (true) {
            delay(100) // 进一步缩短到100ms，提高本地音频状态响应速度
            soundItems.forEach { item ->
                // 从AudioManager获取实际播放状态并同步
                val actualPlaying = audioManager.isPlayingSound(item.sound)
                // 只有当状态确实发生变化时才更新，避免不必要的重组
                if (playingStates[item.sound] != actualPlaying) {
                    playingStates[item.sound] = actualPlaying
                }
            }
        }
    }
    
    // 检查是否有任何声音在播放（本地+远程+本地音频文件）
    var hasAnyPlayingSounds by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            // 使用 AudioManager 的统一方法检查所有音频
            hasAnyPlayingSounds = audioManager.hasAnyPlayingSounds()
            
            // 如果没有声音在播放且倒计时是激活状态，自动取消倒计时
            if (!hasAnyPlayingSounds && isTimerActive) {
                timerManager.cancelTimer()
            }
            
            delay(500) // 每500ms检查一次
        }
    }
    
    // 倒计时监听器
    val timerListener = remember {
        object : TimerManager.TimerListener {
            override fun onTimerTick(timeLeftMillis: Long) {
                // 状态已通过StateFlow更新，这里不需要额外操作
            }
            
            override fun onTimerFinished() {
                // 倒计时自然结束，立即停止所有声音播放（本地和远程）
                // 使用 runBlocking 确保在主线程同步执行，避免异步延迟导致的问题
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        // 停止所有声音（同步执行，确保立即生效）
                        audioManager.stopAllSounds()
                        
                        // 立即更新所有本地声音播放状态
                        soundItems.forEach { item ->
                            playingStates[item.sound] = false
                        }
                        
                        // 立即更新所有远程声音播放状态
                        playingRemoteSounds = emptySet()
                        
                        // 延迟一小段时间后再次验证，确保所有声音都已停止
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            // 再次检查并强制停止任何仍在播放的声音
                            val stillPlaying = audioManager.hasAnyPlayingSounds()
                            if (stillPlaying) {
                                Logger.w("SoundsScreen", "倒计时结束后仍有声音在播放，进行二次停止")
                                audioManager.stopAllSounds()
                                // 再次更新UI状态
                                soundItems.forEach { item ->
                                    playingStates[item.sound] = false
                                }
                                playingRemoteSounds = emptySet()
                            }
                        }, 200) // 200ms后验证
                        
                        // 显示Toast提示
                        android.widget.Toast.makeText(context, context.getString(R.string.countdown_ended_stopped), android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Logger.e("SoundsScreen", "倒计时结束处理失败: ${e.message}", e)
                        // 即使出错也要尝试停止
                        try {
                            audioManager.stopAllSounds()
                        } catch (ex: Exception) {
                            Logger.e("SoundsScreen", "二次停止失败: ${ex.message}")
                        }
                    }
                }
            }
            
            override fun onTimerCancelled() {
                // 倒计时被用户取消，不停止音频播放
                Logger.d("SoundsScreen", "倒计时已取消，继续播放")
                // 什么都不做，让音频继续播放
            }
        }
    }
    
    // 添加倒计时监听器
    DisposableEffect(Unit) {
        timerManager.addListener(timerListener)
        onDispose {
            timerManager.removeListener(timerListener)
        }
    }

    // 滚动状态（用于检测是否正在滚动）
    val builtInScrollState = rememberLazyGridState()
    
    // 默认区域编辑模式状态
    var isDefaultAreaEditMode by remember { mutableStateOf(false) }
    
    // 是否正在滚动
    val isScrolling = builtInScrollState.isScrollInProgress
    
    // 监听滚动状态，触发浮动按钮收缩
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            onScrollDetected()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = contentAlpha } // 应用透明度
            .then(
                // 当透明度为0时，禁用所有点击事件（除了灯泡按钮）
                if (contentAlpha == 0f) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // 顶部标题、深色模式切换按钮和收藏按钮（天气模式下隐藏）
        if (!weatherEnabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：标题和治愈句子
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "XMSLEEP",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                // 治愈句子（根据时段随机显示，应用启动时生成，页面切换时不刷新）
                // 使用 LanguageManager 的当前语言作为 key，确保语言切换时重新生成句子
                val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
                val healingQuote = remember(currentLanguage) { 
                    org.xmsleep.app.quote.HealingQuoteManager.getRandomQuote(context) 
                }
                Text(
                    text = healingQuote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // 右侧：更新图标和收藏按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 更新图标（有新版本时显示，带小红点）
                if (showUpdateIcon) {
                    Box {
                        IconButton(
                            onClick = { showUpdateDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = context.getString(R.string.software_update),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // 小红点Badge - 调整到图标右上角
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp) // 调整位置到图标内部右上角
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                // 番茄时钟按钮
                IconButton(
                    onClick = { onNavigateToFlipClock() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = context.getString(R.string.tomato_timer_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        }
        
        // 快捷播放展开/收缩状态（提前定义，以便在Column中使用）
        // 应用启动时始终收起，不管之前的状态
        var isQuickPlayExpanded by remember { 
            mutableStateOf(false) 
        }
        
        // 协程作用域（用于在pointerInput中更新状态）
        val scope = rememberCoroutineScope()
        
        // 内置页面内容（使用Box包装以支持FAB对齐和底部弹出区域）
        Box(modifier = Modifier.fillMaxSize()) {
            // 实时获取默认卡片列表（当前预设）
            val defaultItems = remember(activePreset, pinnedSounds.value) {
                soundItems.filter { pinnedSounds.value.contains(it.sound) }
            }
            
            // 实时获取远程音频置顶列表（当前预设）
            val defaultRemoteSounds = remember(activePreset, remoteSounds, remotePinned) {
                remoteSounds.filter { remotePinned.contains(it.id) }
            }
            
            // 只要任何预设有内容，预设模块就始终显示，让用户可以自由切换预设
            val hasDefaultItems = hasAnyPresetItems
            
            // 当默认区域没有内容时，自动退出编辑模式
            LaunchedEffect(defaultItems.isEmpty()) {
                if (defaultItems.isEmpty()) {
                    isDefaultAreaEditMode = false
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 天气智能推荐卡片（天气模式下居中显示）
                if (weatherEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        WeatherCard(
                            currentWeather = currentWeather,
                            remoteSounds = remoteSounds,
                            context = context,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                
                // 标题和布局切换按钮 + 内置声音内容（天气模式下隐藏）
                if (!weatherEnabled) {
                // 标题和布局切换按钮（独立一行）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 标题区域
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = context.getString(R.string.white_noise_cards),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // 右侧按钮区域：预设按钮 + 布局切换按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 预设按钮（只在有预设内容时显示，且天气推荐未开启）
                        if (hasAnyPresetItems && !weatherEnabled) {
                            IconButton(
                                onClick = { showPresetDialog = true } // 显示弹窗
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.grass_24px),
                                    contentDescription = "预设",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // 布局切换按钮（2列/3列）
                        IconButton(
                            onClick = { 
                                // 点击布局切换按钮时退出编辑模式
                                isDefaultAreaEditMode = false
                                val newCount = if (columnsCount == 2) 3 else 2
                                onColumnsCountChange(newCount)
                            }
                        ) {
                            Icon(
                                // 3列时使用线性图标ViewAgenda，2列时使用填充图标GridView
                                imageVector = if (columnsCount == 2) Icons.Default.GridView else Icons.Outlined.ViewAgenda,
                                contentDescription = if (columnsCount == 2) context.getString(R.string.switch_to_3_columns) else context.getString(R.string.switch_to_2_columns),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            
            // 内置声音内容
            BuiltInSoundsContent(
                soundItems = soundItems,
                playingStates = playingStates,
                audioManager = audioManager,
                context = context,
                hideAnimation = hideAnimation,
                columnsCount = columnsCount,
                pinnedSounds = pinnedSounds,
                scrollState = builtInScrollState,
                onEditModeReset = { isDefaultAreaEditMode = false },
                onPinnedChange = { sound, isPinned ->
                    // 已在 onSoundPinChange 中处理，这里直接传递同样的逻辑
                    val currentSet = pinnedSounds.value.toMutableSet()
                    if (isPinned) {
                        val totalPinned = currentSet.size + remotePinned.size
                        if (totalPinned >= 10) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.preset_max_reached),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            currentSet.add(sound)
                            pinnedSounds.value = currentSet
                            playingStates[sound] = audioManager.isPlayingSound(sound)
                        }
                    } else {
                        currentSet.remove(sound)
                        pinnedSounds.value = currentSet
                    }
                }
            )
        } // end if (!weatherEnabled || currentWeather == null) - 内置声音模块
        }
        
        // 快捷播放是否有声音（包括本地和远程）
        val defaultAreaHasSounds = pinnedSounds.value.isNotEmpty() || defaultRemoteSounds.isNotEmpty()
        
        // 实时检测快捷播放的播放状态（包括本地和远程）
        var defaultAreaSoundsPlaying by remember { mutableStateOf(false) }
        LaunchedEffect(activePreset, pinnedSounds.value, defaultRemoteSounds, playingStates, playingRemoteSounds) {
            // 立即检查一次状态
            val localPlaying = pinnedSounds.value.any { audioManager.isPlayingSound(it) }
            val remotePlaying = defaultRemoteSounds.any { audioManager.isPlayingRemoteSound(it.id) }
            defaultAreaSoundsPlaying = localPlaying || remotePlaying
            
            // 然后定期更新
            while (true) {
                delay(300) // 每300ms检查一次
                val currentLocalPlaying = pinnedSounds.value.any { audioManager.isPlayingSound(it) }
                val currentRemotePlaying = defaultRemoteSounds.any { audioManager.isPlayingRemoteSound(it.id) }
                val newState = currentLocalPlaying || currentRemotePlaying
                if (defaultAreaSoundsPlaying != newState) {
                    defaultAreaSoundsPlaying = newState
                }
            }
        }
        
        // 预设弹窗
        if (showPresetDialog && hasDefaultItems) {
            Dialog(
                onDismissRequest = { showPresetDialog = false }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f) // 使用95%的屏幕宽度
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 标题栏：左侧标题 + 右侧编辑按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.preset),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            
                            // 编辑按钮
                            if (isDefaultAreaEditMode) {
                                // 编辑模式下显示"完成"
                                TextButton(
                                    onClick = { isDefaultAreaEditMode = false }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(context.getString(R.string.done))
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                // 默认状态显示笔图标
                                IconButton(
                                    onClick = { isDefaultAreaEditMode = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = context.getString(R.string.edit),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // 预设内容区域
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false) // 不强制填充，根据内容自适应
                                .heightIn(max = 500.dp) // 最大高度限制
                        ) {
                            DefaultArea(
                                soundItems = soundItems,
                                pinnedSounds = pinnedSounds,
                                playingStates = playingStates,
                                soundPlayingPreset = soundPlayingPreset,
                                audioManager = audioManager,
                                context = context,
                                isEditMode = isDefaultAreaEditMode,
                                onEditModeChange = { isDefaultAreaEditMode = it },
                                defaultAreaHasSounds = defaultAreaHasSounds,
                                defaultAreaSoundsPlaying = defaultAreaSoundsPlaying,
                                isExpanded = true, // 弹窗中始终展开
                                activePreset = activePreset,
                                onActivePresetChange = onActivePresetChange,
                                showEditButton = false, // 弹窗中隐藏编辑按钮（已移到标题栏）
                                onPinnedChange = { sound, isPinned ->
                                    val currentSet = pinnedSounds.value.toMutableSet()
                                    if (isPinned) {
                                        val totalPinned = currentSet.size + remotePinned.size
                                        if (totalPinned >= 10) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.preset_max_reached),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            currentSet.add(sound)
                                            playingStates[sound] = audioManager.isPlayingSound(sound)
                                            pinnedSounds.value = currentSet
                                        }
                                    } else {
                                        currentSet.remove(sound)
                                        pinnedSounds.value = currentSet
                                    }
                                },
                                remoteSounds = defaultRemoteSounds,
                                remotePinned = remotePinned,
                                downloadingSounds = downloadingSounds,
                                playingRemoteSounds = playingRemoteSounds,
                                onRemotePinnedChange = { soundId, isPinned ->
                                    val newSet = remotePinned.toMutableSet()
                                    if (isPinned) {
                                        val totalPinned = pinnedSounds.value.size + newSet.size
                                        if (totalPinned >= 10) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.preset_max_reached),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            newSet.add(soundId)
                                        }
                                    } else {
                                        newSet.remove(soundId)
                                    }
                                    remotePinned = newSet
                                    org.xmsleep.app.preferences.PreferencesManager.savePresetRemotePinned(context, activePreset, newSet)
                                },
                                onRemoteCardClick = { sound ->
                                    scope.launch {
                                        try {
                                            val cachedFile = cacheManager.getCachedFile(sound.id)
                                            if (cachedFile == null && sound.remoteUrl != null) {
                                                val downloadFlow = cacheManager.downloadAudioWithProgress(
                                                    sound.remoteUrl,
                                                    sound.id
                                                )
                                                downloadFlow.collect { progress ->
                                                    when (progress) {
                                                        is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                            val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                            downloadingSounds = downloadingSounds + (sound.id to percent)
                                                        }
                                                        is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                            downloadingSounds = downloadingSounds - sound.id
                                                            // 等待文件系统同步后再播放
                                                            delay(200)
                                                            val uri = resourceManager.getSoundUri(sound)
                                                            if (uri != null) {
                                                                audioManager.playRemoteSound(context, sound, uri)
                                                            }
                                                            return@collect
                                                        }
                                                        is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                            downloadingSounds = downloadingSounds - sound.id
                                                            android.widget.Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                            return@collect
                                                        }
                                                    }
                                                }
                                            } else {
                                                val uri = resourceManager.getSoundUri(sound)
                                                if (uri != null) {
                                                    if (audioManager.isPlayingRemoteSound(sound.id)) {
                                                        audioManager.pauseRemoteSound(sound.id)
                                                    } else {
                                                        audioManager.playRemoteSound(context, sound, uri)
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, context.getString(R.string.load_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                getSoundDisplayName = { sound -> getSoundDisplayName(sound) },
                                scope = scope,
                                resourceManager = resourceManager
                            )
                        }
                        
                        // 底部按钮：播放/暂停 + 关闭
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 播放/暂停按钮
                            TextButton(
                                onClick = {
                                    if (defaultAreaSoundsPlaying) {
                                        // 暂停所有预设的声音（本地和远程）
                                        pinnedSounds.value.forEach { sound ->
                                            if (audioManager.isPlayingSound(sound)) {
                                                audioManager.pauseSound(sound)
                                                playingStates[sound] = false
                                                soundPlayingPreset.remove(sound)
                                            }
                                        }
                                        defaultRemoteSounds.forEach { sound ->
                                            if (audioManager.isPlayingRemoteSound(sound.id)) {
                                                audioManager.pauseRemoteSound(sound.id)
                                            }
                                        }
                                    } else {
                                        // 播放所有预设的声音（本地和远程）
                                        pinnedSounds.value.forEach { sound ->
                                            if (!audioManager.isPlayingSound(sound)) {
                                                soundPlayingPreset[sound] = activePreset // 记录从哪个预设播放
                                                audioManager.playSound(context, sound)
                                                // 立即更新状态，不延迟
                                                playingStates[sound] = true
                                            }
                                        }
                                        // 远程音频需要先下载，这里只播放已缓存的
                                        scope.launch {
                                            defaultRemoteSounds.filter { remotePinned.contains(it.id) }.forEach { sound ->
                                                if (!audioManager.isPlayingRemoteSound(sound.id)) {
                                                    val uri = resourceManager.getSoundUri(sound)
                                                    if (uri != null) {
                                                        audioManager.playRemoteSound(context, sound, uri)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // 检查是否设置了自动倒计时
                                        val autoCountdownMinutes = org.xmsleep.app.preferences.PreferencesManager.getAutoCountdownMinutes(context)
                                        if (autoCountdownMinutes > 0) {
                                            // 自动启动倒计时
                                            scope.launch {
                                                delay(500) // 等待声音开始播放
                                                timerManager.startTimer(autoCountdownMinutes)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = if (defaultAreaSoundsPlaying) {
                                        context.getString(R.string.pause_preset)
                                    } else {
                                        context.getString(R.string.play_preset)
                                    }
                                )
                            }
                            // 关闭按钮
                            TextButton(onClick = { showPresetDialog = false }) {
                                Text(context.getString(R.string.close))
                            }
                        }
                    }
                }
            }
        }
        
        // 倒计时FAB（滚动时滑出到边缘外）
        val timerFABOffsetX by animateDpAsState(
            targetValue = if (!isScrolling) 0.dp else 60.dp,
            animationSpec = tween(durationMillis = 300),
            label = "timerFABOffsetX"
        )
        
        // 预设播放FAB的偏移动画（滚动时滑出到边缘外）
        val presetPlayFABOffsetX by animateDpAsState(
            targetValue = if (!isScrolling) 0.dp else 60.dp,
            animationSpec = tween(durationMillis = 300),
            label = "presetPlayFABOffsetX"
        )
        
        // 底部导航栏高度（调整后的避让高度）
        val bottomNavBarHeight = 50.dp
        
        // 预设播放FAB的底部间距（在倒计时FAB上方，距离底部导航栏88dp）
        val presetPlayFABBottomPadding by animateDpAsState(
            targetValue = if (hasDefaultItems) {
                bottomNavBarHeight + 88.dp // 距离底部导航栏88dp（补偿底部导航栏上移）
            } else {
                0.dp // 没有预设时不显示
            },
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "presetPlayFABBottomPadding"
        )
        
        // 倒计时FAB的底部间距
        val timerFABBottomPadding by animateDpAsState(
            targetValue = if (hasDefaultItems) {
                // 有预设时：在预设播放FAB下方，间隔16dp
                bottomNavBarHeight + 88.dp + 56.dp + 16.dp
            } else {
                // 没有预设时：距离底部导航栏88dp
                bottomNavBarHeight + 88.dp
            },
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "timerFABBottomPadding"
        )
        
        // 预设播放FAB（只在有预设音频时显示，位于倒计时FAB上方）
        if (hasDefaultItems) {
            PresetPlayFAB(
                isPlaying = defaultAreaSoundsPlaying,
                onClick = {
                    if (defaultAreaSoundsPlaying) {
                        // 暂停所有快捷播放的声音（本地和远程）
                        pinnedSounds.value.forEach { sound ->
                            if (audioManager.isPlayingSound(sound)) {
                                audioManager.pauseSound(sound)
                                playingStates[sound] = false
                                soundPlayingPreset.remove(sound)
                            }
                        }
                        defaultRemoteSounds.forEach { sound ->
                            if (audioManager.isPlayingRemoteSound(sound.id)) {
                                audioManager.pauseRemoteSound(sound.id)
                            }
                        }
                    } else {
                        // 播放所有快捷播放的声音（本地和远程）
                        pinnedSounds.value.forEach { sound ->
                            if (!audioManager.isPlayingSound(sound)) {
                                soundPlayingPreset[sound] = activePreset // 记录从哪个预设播放
                                audioManager.playSound(context, sound)
                                // 立即更新状态，不延迟
                                playingStates[sound] = true
                            }
                        }
                        // 远程音频需要先下载，这里只播放已缓存的
                        scope.launch {
                            defaultRemoteSounds.filter { remotePinned.contains(it.id) }.forEach { sound ->
                                if (!audioManager.isPlayingRemoteSound(sound.id)) {
                                    val uri = resourceManager.getSoundUri(sound)
                                    if (uri != null) {
                                        audioManager.playRemoteSound(context, sound, uri)
                                    }
                                }
                            }
                        }
                        
                        // 检查是否设置了自动倒计时
                        val autoCountdownMinutes = org.xmsleep.app.preferences.PreferencesManager.getAutoCountdownMinutes(context)
                        if (autoCountdownMinutes > 0) {
                            // 自动启动倒计时
                            scope.launch {
                                delay(500) // 等待声音开始播放
                                timerManager.startTimer(autoCountdownMinutes)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = presetPlayFABBottomPadding, end = 16.dp)
                    .offset(x = presetPlayFABOffsetX)
            )
        }
        
        TimerFAB(
            isTimerActive = isTimerActive,
            timeLeftMillis = timeLeftMillis,
            onClick = { showTimerDialog = true },
            enabled = hasAnyPlayingSounds, // 只有在有声音播放时才可点击
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = timerFABBottomPadding, end = 16.dp)
                .offset(x = timerFABOffsetX)
        )
    }
    }
    
    // 倒计时设置对话框
    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onTimerSet = { minutes ->
                if (minutes > 0) {
                    // 只有在有声音播放时才允许设置倒计时
                    val hasPlaying = audioManager.hasAnyPlayingSounds()
                    if (hasPlaying) {
                        timerManager.startTimer(minutes)
                        android.widget.Toast.makeText(context, context.getString(R.string.countdown_set_minutes, minutes), android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, context.getString(R.string.please_play_sound_before_timer), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    timerManager.cancelTimer()
                    android.widget.Toast.makeText(context, context.getString(R.string.countdown_cancelled), android.widget.Toast.LENGTH_SHORT).show()
                }
                showTimerDialog = false
            },
            currentTimerMinutes = if (isTimerActive) timerManager.getCurrentTimerMinutes() else 0
        )
    }
    
    // 更新对话框
    if (showUpdateDialog && updateViewModel != null) {
        val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
        UpdateDialog(
            onDismiss = { showUpdateDialog = false },
            updateViewModel = updateViewModel,
            currentLanguage = currentLanguage
        )
    }
}
