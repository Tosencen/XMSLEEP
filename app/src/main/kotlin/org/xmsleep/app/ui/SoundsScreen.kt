package org.xmsleep.app.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.i18n.LanguageManager
import androidx.compose.runtime.DisposableEffect
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import com.airbnb.lottie.value.LottieFrameInfo

/**
 * 自定义颜色回调，根据原颜色的亮度和饱和度动态映射到主题色系或灰色系
 */
private class ThemeColorCallback(
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
                    val hueDiff = kotlin.math.abs(originalHct.hue - primaryHct.hue)
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
 */
@Composable
fun SoundsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = false,
    darkMode: org.xmsleep.app.DarkModeOption = org.xmsleep.app.DarkModeOption.AUTO,
    onDarkModeChange: (org.xmsleep.app.DarkModeOption) -> Unit = {},
    columnsCount: Int = 2,
    onColumnsCountChange: (Int) -> Unit = {},
    pinnedSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    onNavigateToFavorite: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val audioManager = remember { AudioManager.getInstance() }
    val timerManager = remember { TimerManager.getInstance() }
    val colorScheme = MaterialTheme.colorScheme
    
    // 各声音的播放状态
    val playingStates = remember { mutableStateMapOf<AudioManager.Sound, Boolean>() }
    
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
    
    // 6个声音模块的数据（使用字符串资源以支持语言切换）
    // 使用 configuration.locales 作为依赖，确保语言切换时重新创建
    val soundItems = remember(colorScheme, configuration.locales) {
        listOf(
            SoundItem(
                AudioManager.Sound.RAIN,
                context.getString(R.string.sound_rain),
                R.raw.rain_animation_optimized,
                colorScheme.primary
            ),
            SoundItem(
                AudioManager.Sound.CAMPFIRE,
                context.getString(R.string.sound_campfire),
                R.raw.gouhuo_animation,
                colorScheme.error
            ),
            SoundItem(
                AudioManager.Sound.THUNDER,
                context.getString(R.string.sound_thunder),
                R.raw.dalei_animation,
                colorScheme.secondary
            ),
            SoundItem(
                AudioManager.Sound.CAT_PURRING,
                context.getString(R.string.sound_cat_purring),
                R.raw.cat,
                colorScheme.tertiary
            ),
            SoundItem(
                AudioManager.Sound.BIRD_CHIRPING,
                context.getString(R.string.sound_bird_chirping),
                R.raw.bird,
                colorScheme.primaryContainer
            ),
            SoundItem(
                AudioManager.Sound.NIGHT_INSECTS,
                context.getString(R.string.sound_night_insects),
                R.raw.xishuai,
                colorScheme.secondaryContainer
            )
        )
    }
    
    // 初始化状态
    LaunchedEffect(Unit) {
        soundItems.forEach { item ->
            playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
        }
    }
    
    // 定期更新播放状态
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            soundItems.forEach { item ->
                playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
            }
        }
    }
    
    // 倒计时监听器
    val timerListener = remember {
        object : TimerManager.TimerListener {
            override fun onTimerTick(timeLeftMillis: Long) {
                // 状态已通过StateFlow更新，这里不需要额外操作
            }
            
            override fun onTimerFinished() {
                // 倒计时结束，立即停止所有声音播放
                audioManager.stopAllSounds()
                // 立即更新所有播放状态
                soundItems.forEach { item ->
                    playingStates[item.sound] = false
                }
                // 在主线程显示Toast
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, context.getString(R.string.countdown_ended_stopped), android.widget.Toast.LENGTH_SHORT).show()
                }
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
    
    // 批量选择模式状态（用于批量设置默认）
    var isBatchSelectMode by remember { mutableStateOf(false) }
    var selectedSoundsForBatch by remember { mutableStateOf(mutableSetOf<AudioManager.Sound>()) }
    
    // 是否正在滚动
    val isScrolling = builtInScrollState.isScrollInProgress

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标题、深色模式切换按钮和收藏按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：标题和深色模式切换按钮
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "XMSLEEP",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 深色/浅色模式切换按钮
                val isDarkMode = when (darkMode) {
                    org.xmsleep.app.DarkModeOption.LIGHT -> false
                    org.xmsleep.app.DarkModeOption.DARK -> true
                    org.xmsleep.app.DarkModeOption.AUTO -> isSystemInDarkTheme()
                }
                
                Surface(
                    onClick = {
                        // 在浅色和深色之间切换（跳过AUTO）
                        val newMode = if (isDarkMode) {
                            org.xmsleep.app.DarkModeOption.LIGHT
                        } else {
                            org.xmsleep.app.DarkModeOption.DARK
                        }
                        onDarkModeChange(newMode)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) context.getString(R.string.switch_to_light_mode) else context.getString(R.string.switch_to_dark_mode),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // 右侧：收藏按钮
            IconButton(
                onClick = { onNavigateToFavorite() }
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = context.getString(R.string.tab_favorite),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 快捷播放展开/收缩状态（提前定义，以便在Column中使用）
        var isQuickPlayExpanded by remember { mutableStateOf(true) }
        
        // 协程作用域（用于在pointerInput中更新状态）
        val scope = rememberCoroutineScope()
        
        // 内置页面内容（使用Box包装以支持FAB对齐和底部弹出区域）
        Box(modifier = Modifier.fillMaxSize()) {
            // 实时获取默认卡片列表（用于判断是否显示默认区域）
            val defaultItems = remember {
                derivedStateOf {
                    soundItems.filter { pinnedSounds.value.contains(it.sound) }
                }
            }.value
            
            // 当默认区域没有内容时，自动退出编辑模式
            LaunchedEffect(defaultItems.isEmpty()) {
                if (defaultItems.isEmpty()) {
                    isDefaultAreaEditMode = false
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isQuickPlayExpanded) {
                        // 当快捷播放模块展开时，点击内容区域的空白处自动收缩
                        if (isQuickPlayExpanded) {
                            detectTapGestures { tapOffset ->
                                // 点击内容区域时，收缩快捷播放模块
                                // 注意：这里不检查点击位置是否在快捷播放模块内，
                                // 因为快捷播放模块在上层，会优先接收点击事件
                                // 如果点击到了这里，说明点击的是内容区域
                                scope.launch {
                                    isQuickPlayExpanded = false
                                }
                            }
                        }
                    }
            ) {
                // 标题和布局切换按钮（独立一行）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBatchSelectMode) {
                            // 批量选择模式下显示关闭按钮和已选择数量
                            IconButton(
                                onClick = {
                                    // 退出批量选择模式（不执行批量操作）
                                    isBatchSelectMode = false
                                    selectedSoundsForBatch.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = context.getString(R.string.cancel_batch_select),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = context.getString(R.string.selected_count, selectedSoundsForBatch.size),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.max_select_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = context.getString(R.string.white_noise_cards),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = context.getString(R.string.builtin_sounds_offline_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                                // 图书钉图标 - 批量选择模式切换（只在非批量选择模式下显示）
                                if (!isBatchSelectMode) {
                                    IconButton(
                                        onClick = { 
                                            // 进入批量选择模式
                                            isBatchSelectMode = true
                                            // 初始化：已设为默认的声音自动选中
                                            selectedSoundsForBatch = pinnedSounds.value.toMutableSet()
                                            // 清除编辑模式
                                            isDefaultAreaEditMode = false
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PushPin,
                                            contentDescription = context.getString(R.string.batch_select_default),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                // 批量选择模式下的完成按钮
                                if (isBatchSelectMode) {
                                    Surface(
                                        onClick = {
                                            // 执行批量操作并退出
                                            val currentSet = pinnedSounds.value.toMutableSet()
                                            val allSounds = soundItems.map { it.sound }.toSet()
                                            
                                            // 先计算将要添加的数量
                                            var addCount = 0
                                            allSounds.forEach { sound ->
                                                val isSelected = selectedSoundsForBatch.contains(sound)
                                                val isCurrentlyPinned = currentSet.contains(sound)
                                                if (isSelected && !isCurrentlyPinned) {
                                                    addCount++
                                                }
                                            }
                                            
                                            // 检查是否超过最大数量（3个）
                                            if (currentSet.size + addCount > 3) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.max_3_sounds_limit),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                // 遍历所有声音，根据选中状态更新默认列表
                                                allSounds.forEach { sound ->
                                                    val isSelected = selectedSoundsForBatch.contains(sound)
                                                    val isCurrentlyPinned = currentSet.contains(sound)
                                                    
                                                    if (isSelected && !isCurrentlyPinned) {
                                                        // 选中且不在默认列表中，添加到默认列表
                                                        currentSet.add(sound)
                                                    } else if (!isSelected && isCurrentlyPinned) {
                                                        // 未选中但在默认列表中，从默认列表中移除
                                                        currentSet.remove(sound)
                                                    }
                                                }
                                                
                                                pinnedSounds.value = currentSet
                                                // 立即同步播放状态
                                                currentSet.forEach { sound ->
                                                    playingStates[sound] = audioManager.isPlayingSound(sound)
                                                }
                                                
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.default_area_updated),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                
                                                // 退出批量选择模式
                                                isBatchSelectMode = false
                                                selectedSoundsForBatch.clear()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = context.getString(R.string.done),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                
                                // 布局切换按钮
                                IconButton(
                                    onClick = { 
                                        // 点击布局切换按钮时退出编辑模式和批量选择模式
                                        isDefaultAreaEditMode = false
                                        if (isBatchSelectMode) {
                                            isBatchSelectMode = false
                                            selectedSoundsForBatch.clear()
                                        }
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
                favoriteSounds = favoriteSounds,
                isBatchSelectMode = isBatchSelectMode,
                selectedSoundsForBatch = selectedSoundsForBatch,
                scrollState = builtInScrollState,
                onSoundBatchSelect = { sound ->
                    val newSet = selectedSoundsForBatch.toMutableSet()
                    if (newSet.contains(sound)) {
                        // 取消选择
                        newSet.remove(sound)
                        selectedSoundsForBatch = newSet
                    } else {
                        // 尝试选择新卡片
                        if (newSet.size >= 3) {
                            // 已经选择了3个，不允许再选择
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.max_3_sounds_limit),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // 可以选择
                            newSet.add(sound)
                            selectedSoundsForBatch = newSet
                        }
                    }
                },
                onEditModeReset = { isDefaultAreaEditMode = false },
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
        
        // 快捷播放是否有声音
        val defaultAreaHasSounds = pinnedSounds.value.isNotEmpty()
        
        // 实时检测快捷播放的播放状态
        var defaultAreaSoundsPlaying by remember { mutableStateOf(false) }
        LaunchedEffect(pinnedSounds.value, Unit) {
            while (true) {
                defaultAreaSoundsPlaying = pinnedSounds.value.any { audioManager.isPlayingSound(it) }
                kotlinx.coroutines.delay(300) // 每300ms检查一次
            }
        }
        
        // 底部弹出区域的高度偏移动画（从底部滑入）
        // Material Design 3 NavigationBar（有标签）标准高度是80dp
        // Scaffold的bottomBar会在paddingValues中添加80dp的底部padding
        // 由于SoundsScreen的modifier已经应用了paddingValues，Box的底部对齐到这个padding后的容器
        // 所以需要向上偏移80dp来紧贴NavigationBar
        val bottomSheetOffsetY by animateDpAsState(
            targetValue = if (defaultItems.isNotEmpty()) {
                0.dp // 不偏移，紧贴底部导航栏
            } else {
                300.dp
            },
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "bottomSheetOffsetY"
        )
        
        // 底部弹出的快捷播放（悬浮在内容之上，位于底部导航栏上方）
        if (defaultItems.isNotEmpty()) {
            val density = LocalDensity.current
            val dragThreshold = with(density) { 20.dp.toPx() } // 滑动阈值20dp（降低阈值，更容易触发）
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = bottomSheetOffsetY)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .pointerInput(isQuickPlayExpanded) {
                        var totalDragY = 0f
                        
                        detectVerticalDragGestures(
                            onDragStart = {
                                // 滑动开始时重置累计距离
                                totalDragY = 0f
                            },
                            onDragEnd = {
                                // 滑动结束时，根据总滑动距离判断是否切换状态
                                val finalDragY = totalDragY
                                totalDragY = 0f
                                
                                if (kotlin.math.abs(finalDragY) >= dragThreshold) {
                                    // 向上滑（负值）表示收缩，向下滑（正值）表示展开
                                    scope.launch {
                                        // 在协程中实时获取当前状态
                                        val currentExpanded = isQuickPlayExpanded
                                        if (finalDragY < 0 && currentExpanded) {
                                            // 向上滑动且当前展开，则收缩
                                            isQuickPlayExpanded = false
                                        } else if (finalDragY > 0 && !currentExpanded) {
                                            // 向下滑动且当前收缩，则展开
                                            isQuickPlayExpanded = true
                                        }
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            // 累计滑动距离（只累计垂直方向的滑动）
                            totalDragY += dragAmount
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 顶部指示栏 - 独立的拖拽栏，用于展开/收缩（在标题栏上方）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = { isQuickPlayExpanded = !isQuickPlayExpanded },
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isQuickPlayExpanded) 
                                        Icons.Default.ExpandMore 
                                    else 
                                        Icons.Default.ExpandLess,
                                    contentDescription = if (isQuickPlayExpanded) "收缩" else "展开",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    
                    // 快捷播放内容区域（包含标题、按钮、卡片等）
                    DefaultArea(
                    soundItems = soundItems,
                    pinnedSounds = pinnedSounds,
                    favoriteSounds = favoriteSounds,
                    playingStates = playingStates,
                    audioManager = audioManager,
                    context = context,
                    isEditMode = isDefaultAreaEditMode,
                    onEditModeChange = { isDefaultAreaEditMode = it },
                    defaultAreaHasSounds = defaultAreaHasSounds,
                    defaultAreaSoundsPlaying = defaultAreaSoundsPlaying,
                    isExpanded = isQuickPlayExpanded,
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
                                // 如果声音正在播放，立即同步播放状态
                                playingStates[sound] = audioManager.isPlayingSound(sound)
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
                    },
                    onEnterBatchSelectMode = {
                        // 进入批量选择模式
                        isBatchSelectMode = true
                        // 初始化：已设为默认的声音自动选中
                        selectedSoundsForBatch = pinnedSounds.value.toMutableSet()
                        // 清除编辑模式
                        isDefaultAreaEditMode = false
                    }
                )
                }
            }
        }
        
        // 倒计时FAB（批量选择模式或滚动时滑出到边缘外）
        val timerFABOffsetX by animateDpAsState(
            targetValue = if (!isBatchSelectMode && !isScrolling) 0.dp else 60.dp,
            animationSpec = tween(durationMillis = 300),
            label = "timerFABOffsetX"
        )
        
        // 倒计时按钮的底部padding动画（与快捷播放保持30dp间隔）
        // 顶部箭头栏：44dp（12dp padding + 32dp图标）
        // 标题栏：约72dp
        // 卡片区域（展开时）：约104dp
        // 收缩状态高度：44dp + 72dp = 116dp
        // 展开状态高度：44dp + 72dp + 104dp = 220dp
        // 动画配置与快捷播放模块的AnimatedVisibility保持一致（300ms）
        val arrowBarHeight = 44.dp // 顶部箭头栏高度
        val titleBarHeight = 72.dp // 标题栏高度
        val cardAreaHeight = 104.dp // 卡片区域高度
        val quickPlayHeight = if (isQuickPlayExpanded) {
            arrowBarHeight + titleBarHeight + cardAreaHeight // 展开状态
        } else {
            arrowBarHeight + titleBarHeight // 收缩状态（只有标题栏）
        }
        val timerFABBottomPadding by animateDpAsState(
            targetValue = if (defaultItems.isNotEmpty()) {
                quickPlayHeight + 30.dp // 快捷播放高度 + 30dp间隔（从FAB底部到快捷播放顶部）
            } else {
                16.dp // 复位到原来的位置
            },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "timerFABBottomPadding"
        )
        
        TimerFAB(
            isTimerActive = isTimerActive,
            timeLeftMillis = timeLeftMillis,
            onClick = { showTimerDialog = true },
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
                    // 如果当前没有声音播放，自动播放第一个声音（雨声）
                    if (!hasPlayingSound) {
                        audioManager.playSound(context, AudioManager.Sound.RAIN)
                        soundItems.forEach { item ->
                            if (item.sound == AudioManager.Sound.RAIN) {
                                playingStates[item.sound] = true
                            }
                        }
                    }
                    timerManager.startTimer(minutes)
                    android.widget.Toast.makeText(context, context.getString(R.string.countdown_set_minutes, minutes), android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    timerManager.cancelTimer()
                    android.widget.Toast.makeText(context, context.getString(R.string.countdown_cancelled), android.widget.Toast.LENGTH_SHORT).show()
                }
                showTimerDialog = false
            },
            currentTimerMinutes = if (isTimerActive) timerManager.getCurrentTimerMinutes() else 0
        )
    }
}

/**
 * 默认区域组件
 */
@Composable
private fun DefaultArea(
    soundItems: List<SoundItem>,
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<AudioManager.Sound>>,
    playingStates: MutableMap<AudioManager.Sound, Boolean>,
    audioManager: AudioManager,
    context: android.content.Context,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    defaultAreaHasSounds: Boolean,
    defaultAreaSoundsPlaying: Boolean,
    isExpanded: Boolean = true,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit,
    onEnterBatchSelectMode: () -> Unit = {}
) {
    // 实时获取默认卡片列表（使用derivedStateOf确保状态变化时触发重组）
    val defaultItems = remember {
        derivedStateOf {
            soundItems.filter { pinnedSounds.value.contains(it.sound) }
        }
    }.value
    
    Column(modifier = Modifier
        .fillMaxWidth()
        // 移除底部padding，让内容紧贴底部导航栏
    ) {
        // 标题和编辑按钮（一行）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), // 顶部增加间距
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = context.getString(R.string.default_playback_area),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = context.getString(R.string.click_play_button_to_play_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 右侧按钮区域：编辑按钮和播放按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp), // 增加到30dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 编辑按钮（在播放按钮左侧）
                // 收缩状态下不可点击，只有展开状态下才能使用
                val editButtonAlpha = if (isExpanded) 1f else 0.4f
                if (isEditMode) {
                    // 编辑模式下显示"完成"文字+图标
                    Surface(
                        onClick = { 
                            if (isExpanded) {
                                onEditModeChange(!isEditMode)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .height(40.dp) // 固定高度与IconButton一致
                            .alpha(editButtonAlpha)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = context.getString(R.string.done),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    // 默认状态只显示笔图标
                    IconButton(
                        onClick = { 
                            if (isExpanded) {
                                onEditModeChange(!isEditMode)
                            }
                        },
                        enabled = isExpanded,
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(editButtonAlpha)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = context.getString(R.string.edit),
                            tint = if (isExpanded) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
                
                // 播放按钮（放在最右侧，添加背景使其更明显）
                if (defaultAreaHasSounds) {
                    Surface(
                        onClick = {
                            if (defaultAreaSoundsPlaying) {
                                // 暂停所有快捷播放的声音
                                pinnedSounds.value.forEach { sound ->
                                    if (audioManager.isPlayingSound(sound)) {
                                        audioManager.pauseSound(sound)
                                    }
                                }
                            } else {
                                // 播放所有快捷播放的声音
                                pinnedSounds.value.forEach { sound ->
                                    if (!audioManager.isPlayingSound(sound)) {
                                        audioManager.playSound(context, sound)
                                    }
                                }
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (defaultAreaSoundsPlaying) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = if (defaultAreaSoundsPlaying) context.getString(R.string.pause) else context.getString(R.string.play),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 默认区域容器（移除Surface，因为已经在外部Box中添加了背景）
        // 使用 AnimatedVisibility 实现平滑的展开/收缩动画
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            ) {
            if (defaultItems.isEmpty()) {
                // 没有默认卡片时，显示占位区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateAnimation(size = 72.dp)
                }
            } else {
                // 有默认卡片时，显示卡片（固定3列，只显示标题和声音图标）
                // 计算需要显示的占位图数量（始终显示3个位置）
                val placeholderCount = 3 - defaultItems.size
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 显示实际卡片
                    items(defaultItems) { item ->
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        
                        DefaultCard(
                            item = item,
                            isPlaying = playingStates[item.sound] ?: false,
                            isFavorite = favoriteSounds.value.contains(item.sound),
                            isEditMode = isEditMode,
                            onToggle = { sound ->
                                val wasPlaying = audioManager.isPlayingSound(sound)
                                if (wasPlaying) {
                                    audioManager.pauseSound(sound)
                                    playingStates[sound] = false
                                } else {
                                    audioManager.playSound(context, sound)
                                    playingStates[sound] = true
                                }
                            },
                            onRemove = {
                                val currentSet = pinnedSounds.value.toMutableSet()
                                currentSet.remove(item.sound)
                                pinnedSounds.value = currentSet
                            },
                            onPinnedChange = { isPinned ->
                                val currentSet = pinnedSounds.value.toMutableSet()
                                if (isPinned) {
                                    currentSet.add(item.sound)
                                    // 如果声音正在播放，立即同步播放状态
                                    playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                                } else {
                                    currentSet.remove(item.sound)
                                }
                                pinnedSounds.value = currentSet
                                onPinnedChange(item.sound, isPinned)
                            },
                            onFavoriteChange = { isFavorite ->
                                val currentSet = favoriteSounds.value.toMutableSet()
                                if (isFavorite) {
                                    currentSet.add(item.sound)
                                } else {
                                    currentSet.remove(item.sound)
                                }
                                favoriteSounds.value = currentSet
                                onFavoriteChange(item.sound, isFavorite)
                            }
                        )
                        
                        // 音量调节对话框
                        if (showVolumeDialog) {
                            VolumeDialog(
                                sound = item.sound,
                                currentVolume = audioManager.getVolume(item.sound),
                                onDismiss = { showVolumeDialog = false },
                                onVolumeChange = { volume ->
                                    audioManager.setVolume(item.sound, volume)
                                }
                            )
                        }
                    }
                    
                    // 显示占位图（填充剩余位置）
                    items(placeholderCount) {
                        PlaceholderCard(
                            onClick = onEnterBatchSelectMode
                        )
                    }
                }
            }
        }
            }
    }
}

/**
 * 占位卡片组件（显示➕号图标）
 */
@Composable
private fun PlaceholderCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * 默认卡片组件（简化版，只显示标题和声音图标）
 */
@Composable
private fun DefaultCard(
    modifier: Modifier = Modifier,
    item: SoundItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isEditMode: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onRemove: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(enabled = !isEditMode) { onToggle(item.sound) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 删除按钮（右下角，只在编辑模式下显示，距离底部20dp）
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
            
            // 标题（左上角）
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .alpha(alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // 音频可视化器（左下角，三条竖线动画，只在播放时显示）
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
        }
    }
}

/**
 * 内置声音内容
 */
@Composable
private fun BuiltInSoundsContent(
    soundItems: List<SoundItem>,
    playingStates: MutableMap<AudioManager.Sound, Boolean>,
    audioManager: AudioManager,
    context: android.content.Context,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<AudioManager.Sound>>,
    isBatchSelectMode: Boolean = false,
    selectedSoundsForBatch: MutableSet<AudioManager.Sound>,
    scrollState: LazyGridState,
    onSoundBatchSelect: (AudioManager.Sound) -> Unit = {},
    onEditModeReset: () -> Unit,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 显示所有卡片（不过滤默认卡片，因为默认是复制而不是移动）
        val normalItems = soundItems
        
        // 白噪音卡片区域
        if (normalItems.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                state = scrollState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(normalItems) { item ->
                    var showVolumeDialog by remember { mutableStateOf(false) }
                    
                    SoundCard(
                        item = item,
                        isPlaying = playingStates[item.sound] ?: false,
                        hideAnimation = hideAnimation,
                        columnsCount = columnsCount,
                        isPinned = pinnedSounds.value.contains(item.sound),
                        isFavorite = favoriteSounds.value.contains(item.sound),
                        isBatchSelectMode = isBatchSelectMode,
                isSelected = selectedSoundsForBatch.contains(item.sound),
                canSelect = selectedSoundsForBatch.size < 3 || selectedSoundsForBatch.contains(item.sound),
                onToggle = { sound ->
                            if (isBatchSelectMode) {
                                // 批量选择模式下，切换选中状态
                                onSoundBatchSelect(sound)
                            } else {
                                // 点击卡片时退出编辑模式
                                onEditModeReset()
                                val wasPlaying = audioManager.isPlayingSound(sound)
                                if (wasPlaying) {
                                    audioManager.pauseSound(sound)
                                    playingStates[sound] = false
                                } else {
                                    audioManager.playSound(context, sound)
                                    playingStates[sound] = true
                                }
                            }
                        },
                        onVolumeClick = { showVolumeDialog = true },
                        onTitleClick = { },
                        onPinnedChange = { isPinned ->
                            val currentSet = pinnedSounds.value.toMutableSet()
                            if (isPinned) {
                                currentSet.add(item.sound)
                                // 如果声音正在播放，立即同步播放状态
                                playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                            } else {
                                currentSet.remove(item.sound)
                            }
                            pinnedSounds.value = currentSet
                            onPinnedChange(item.sound, isPinned)
                        },
                        onFavoriteChange = { isFavorite ->
                            val currentSet = favoriteSounds.value.toMutableSet()
                            if (isFavorite) {
                                currentSet.add(item.sound)
                            } else {
                                currentSet.remove(item.sound)
                            }
                            favoriteSounds.value = currentSet
                            onFavoriteChange(item.sound, isFavorite)
                        }
                    )
                    
                    // 音量调节对话框
                    if (showVolumeDialog) {
                        VolumeDialog(
                            sound = item.sound,
                            currentVolume = audioManager.getVolume(item.sound),
                            onDismiss = { showVolumeDialog = false },
                            onVolumeChange = { volume ->
                                audioManager.setVolume(item.sound, volume)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 收藏声音内容
 */
@Composable
private fun FavoriteSoundsContent(
    soundItems: List<SoundItem>,
    playingStates: MutableMap<AudioManager.Sound, Boolean>,
    audioManager: AudioManager,
    context: android.content.Context,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<AudioManager.Sound>>,
    scrollState: LazyGridState,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit
) {
    // 过滤出收藏的声音
    val favoriteItems = remember(favoriteSounds.value) {
        soundItems.filter { favoriteSounds.value.contains(it.sound) }
    }
    
    if (favoriteItems.isEmpty()) {
        // 没有收藏时显示占位符
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EmptyStateAnimation(size = 240.dp)
                Text(
                    context.getString(R.string.no_favorites),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    context.getString(R.string.favorites_will_show_here),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // 有收藏时显示卡片列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(favoriteItems) { item ->
                var showVolumeDialog by remember { mutableStateOf(false) }
                
                SoundCard(
                    item = item,
                    isPlaying = playingStates[item.sound] ?: false,
                    hideAnimation = hideAnimation,
                    columnsCount = columnsCount,
                    isPinned = pinnedSounds.value.contains(item.sound),
                    isFavorite = favoriteSounds.value.contains(item.sound),
                    onToggle = { sound ->
                        val wasPlaying = audioManager.isPlayingSound(sound)
                        if (wasPlaying) {
                            audioManager.pauseSound(sound)
                            playingStates[sound] = false
                        } else {
                            audioManager.playSound(context, sound)
                            playingStates[sound] = true
                        }
                    },
                    onVolumeClick = { showVolumeDialog = true },
                    onTitleClick = { },
                    onPinnedChange = { isPinned ->
                        val currentSet = pinnedSounds.value.toMutableSet()
                        if (isPinned) {
                            currentSet.add(item.sound)
                            // 如果声音正在播放，立即同步播放状态
                            playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                        } else {
                            currentSet.remove(item.sound)
                        }
                        pinnedSounds.value = currentSet
                        onPinnedChange(item.sound, isPinned)
                    },
                    onFavoriteChange = { isFavorite ->
                        val currentSet = favoriteSounds.value.toMutableSet()
                        if (isFavorite) {
                            currentSet.add(item.sound)
                        } else {
                            currentSet.remove(item.sound)
                        }
                        favoriteSounds.value = currentSet
                        onFavoriteChange(item.sound, isFavorite)
                    }
                )
                
                // 音量调节对话框
                if (showVolumeDialog) {
                    VolumeDialog(
                        sound = item.sound,
                        currentVolume = audioManager.getVolume(item.sound),
                        onDismiss = { showVolumeDialog = false },
                        onVolumeChange = { volume ->
                            audioManager.setVolume(item.sound, volume)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 线上声音内容
 */
@Composable
private fun OnlineSoundsContent() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmptyStateAnimation(size = 200.dp)
            Text(
                context.getString(R.string.no_online_content),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                context.getString(R.string.online_sounds_will_show_here),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 声音卡片
 */
@Composable
fun SoundCard(
    item: SoundItem,
    isPlaying: Boolean,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    isBatchSelectMode: Boolean = false,
    isSelected: Boolean = false,
    canSelect: Boolean = true,
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 标题弹窗状态
    var showTitleMenu by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    // 获取当前主题颜色（在Composable上下文中）
    val currentPrimaryColor = MaterialTheme.colorScheme.primary
    val currentOutlineColor = MaterialTheme.colorScheme.outline
    
    // 批量选择模式下，如果不可选择，降低透明度
    val cardAlpha by animateFloatAsState(
        targetValue = if (isBatchSelectMode && !canSelect && !isSelected) 0.5f else 1f,
        label = "cardAlpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (hideAnimation) 100.dp else 140.dp)
            .alpha(cardAlpha)
            .pointerInput(isBatchSelectMode, isSelected, canSelect) {
                detectTapGestures(
                    onTap = {
                        if (isBatchSelectMode) {
                            // 批量选择模式下：已选中的卡片总是可以点击来取消选择
                            if (isSelected || canSelect) {
                                onToggle(item.sound)
                            }
                        } else {
                            // 普通模式下：点击卡片播放/暂停
                            onToggle(item.sound)
                        }
                    },
                    onLongPress = {
                        // 长按卡片：显示菜单
                        if (!isBatchSelectMode) {
                            showTitleMenu = true
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 批量选择模式下的选择框（右上角）
            if (isBatchSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isSelected) context.getString(R.string.selected) else context.getString(R.string.unselected),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hideAnimation) {
                // 隐藏动画时的布局
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (columnsCount == 3) {
                        // 3列时：标题在左上角，音量图标在右下角
                        // 标题（左上角，不再可点击）
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.alpha(alpha)
                            )
                        }
                        
                        // 菜单弹窗（统一使用 showTitleMenu）
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp)
                        ) {
                            // 默认选项
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
                        }
                        
                        // 音频可视化器（左下角，三条竖线动画，只在播放时显示）
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
                        
                        // 音量图标（右下角，只在播放时显示）
                        if (isPlaying) {
                            IconButton(
                                onClick = onVolumeClick,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 12.dp) // 向右偏移10dp，向下偏移12dp
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
                    } else {
                        // 2列时：标题在左上角，音量图标在右下角
                        // 标题（左上角，不再可点击）
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.alpha(alpha)
                            )
                        }
                        
                        // 菜单弹窗（统一使用 showTitleMenu）
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp)
                        ) {
                            // 默认选项
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
                        }
                        
                        // 音频可视化器（左下角，三条竖线动画，只在播放时显示）
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
                        
                        // 音量图标（右下角，只在播放时显示）
                        if (isPlaying) {
                            IconButton(
                                onClick = onVolumeClick,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 10.dp, y = 12.dp) // 向右偏移10dp，向下偏移12dp
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
                }
            } else {
                // 显示动画时的原始布局
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    
                    // 标题（左上角，不再可点击）
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alpha(alpha)
                        )
                    }
                    
                    // 菜单弹窗（统一使用 showTitleMenu）
                    DropdownMenu(
                        expanded = showTitleMenu,
                        onDismissRequest = { showTitleMenu = false },
                        modifier = Modifier.width(120.dp)
                    ) {
                        // 默认选项
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
                    }
                    
                    // Lottie动画（左下角）
                    AndroidView(
                        factory = { ctx ->
                            LottieAnimationView(ctx).apply {
                                setAnimation(item.animationRes)
                                repeatCount = LottieDrawable.INFINITE
                            }
                        },
                        update = { view ->
                            view.alpha = alpha
                            
                            // 应用填充主题色（使用主色调）
                            val primaryArgb = currentPrimaryColor.toArgb()
                            view.addValueCallback(
                                KeyPath("**"),
                                LottieProperty.COLOR,
                                LottieValueCallback(primaryArgb)
                            )
                            
                            // 应用描边主题色（使用outline颜色）
                            val outlineArgb = currentOutlineColor.toArgb()
                            view.addValueCallback(
                                KeyPath("**"),
                                LottieProperty.STROKE_COLOR,
                                LottieValueCallback(outlineArgb)
                            )
                            
                            // 根据播放状态控制动画
                            if (isPlaying) {
                                if (!view.isAnimating) {
                                    view.resumeAnimation()
                                }
                            } else {
                                // 未播放时，动画静止在第一帧
                                view.pauseAnimation()
                                view.progress = 0f
                                // 强制刷新视图，确保颜色正确应用到第一帧
                                view.invalidate()
                                // 确保在静止状态下颜色回调仍然生效
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    val primaryArgb = currentPrimaryColor.toArgb()
                                    val outlineArgb = currentOutlineColor.toArgb()
                                    view.addValueCallback(
                                        KeyPath("**"),
                                        LottieProperty.COLOR,
                                        LottieValueCallback(primaryArgb)
                                    )
                                    view.addValueCallback(
                                        KeyPath("**"),
                                        LottieProperty.STROKE_COLOR,
                                        LottieValueCallback(outlineArgb)
                                    )
                                    view.invalidate()
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(80.dp)
                    )
                }
                
                // 音量图标（右下角，只在播放时显示）
                if (isPlaying) {
                    IconButton(
                        onClick = onVolumeClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = -4.dp) // 向上偏移4dp，距离底部有适当间距
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
        }
    }
}

/**
 * 标题弹窗菜单
 */
@Composable
private fun TitleMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    isPinned: Boolean,
    isFavorite: Boolean,
    onPinnedClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    if (showMenu) {
        // 使用 DropdownMenu 来显示弹窗
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(120.dp)
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
                        text = if (isPinned) "取消默认" else "默认",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    }
                },
                onClick = onPinnedClick
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
                            text = if (isFavorite) "取消收藏" else "收藏",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                onClick = onFavoriteClick
            )
        }
    }
}

/**
 * 倒计时FAB组件
 * 完全匹配首页FAB的样式：使用Material3默认值，不自定义任何参数
 */
@Composable
private fun TimerFAB(
    isTimerActive: Boolean,
    timeLeftMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 使用key确保当timeLeftMillis变化时强制重组
    key(timeLeftMillis, isTimerActive) {
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = onClick
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = context.getString(R.string.set_countdown)
                )
            }
            
            // 倒计时激活时显示时间Badge
            if (isTimerActive && timeLeftMillis > 0) {
                // 直接计算，不使用remember，确保每次timeLeftMillis变化时都重新计算
                val totalHours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis)
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis)
                val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis)
                
                val hours = totalHours
                val minutes = totalMinutes % 60
                val seconds = totalSeconds % 60
                
                val timerText = when {
                    hours > 0 -> {
                        // 超过1小时：显示"小时:分钟:秒"
                        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                    }
                    else -> {
                        // 少于1小时：显示"分钟:秒"
                        "${minutes}:${seconds.toString().padStart(2, '0')}"
                    }
                }
                
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 音量调节对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeDialog(
    sound: AudioManager.Sound,
    currentVolume: Float,
    onDismiss: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    val context = LocalContext.current
    var volume by remember { mutableStateOf(currentVolume) }
    
    val soundName = when (sound) {
        AudioManager.Sound.RAIN -> context.getString(R.string.sound_rain)
        AudioManager.Sound.CAMPFIRE -> context.getString(R.string.sound_campfire)
        AudioManager.Sound.THUNDER -> context.getString(R.string.sound_thunder)
        AudioManager.Sound.CAT_PURRING -> context.getString(R.string.sound_cat_purring)
        AudioManager.Sound.BIRD_CHIRPING -> context.getString(R.string.sound_bird_chirping)
        AudioManager.Sound.NIGHT_INSECTS -> context.getString(R.string.sound_night_insects)
        else -> context.getString(R.string.sound_default)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.adjust_volume_for, soundName)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 音量滑块
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Slider(
                        value = volume,
                        onValueChange = { 
                            volume = it
                            onVolumeChange(it)
                        },
                        modifier = Modifier.weight(1f),
                        valueRange = 0f..1f,
                        steps = 19  // 0到100，步长5%
                    )
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(50.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok))
            }
        }
    )
}

/**
 * 倒计时设置对话框
 * 符合项目UI规范：简洁的AlertDialog样式，与VolumeDialog保持一致
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerDialog(
    onDismiss: () -> Unit,
    onTimerSet: (Int) -> Unit,
    currentTimerMinutes: Int = 0
) {
    val context = LocalContext.current
    var selectedMinutes by remember { mutableStateOf(if (currentTimerMinutes > 0) currentTimerMinutes else 30) }
    
    // 预设时间选项（分钟）
    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.set_countdown)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 当前倒计时状态提示
                if (currentTimerMinutes > 0) {
                    val hours = currentTimerMinutes / 60
                    val mins = currentTimerMinutes % 60
                    val statusText = if (hours > 0) {
                        if (mins > 0) {
                            context.getString(R.string.hours_minutes, hours, mins)
                        } else {
                            context.getString(R.string.hours_minutes, hours, 0)
                        }
                    } else {
                        context.getString(R.string.minutes_only, mins)
                    }
                    Text(
                        text = context.getString(R.string.current_countdown, statusText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 预设时间快速选择
                Text(
                    context.getString(R.string.quick_select),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 将6个选项分成两行，每行3个
                    val rows = presetMinutes.chunked(3)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { mins ->
                                val isSelected = selectedMinutes == mins
                                FilterChip(
                                    onClick = { selectedMinutes = mins },
                                    label = {
                                        Text(context.getString(R.string.minutes, mins))
                                    },
                                    selected = isSelected,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果一行不满3个，添加占位符保持布局
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // 时间选择滑块
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt() },
                        valueRange = 5f..180f,
                        steps = 34, // 每5分钟一个步长 (5, 10, 15, ..., 180)
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = context.getString(R.string.five_minutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (selectedMinutes >= 60) {
                                val hours = selectedMinutes / 60
                                val mins = selectedMinutes % 60
                                if (mins > 0) {
                                    context.getString(R.string.hours_minutes, hours, mins)
                                } else {
                                    context.getString(R.string.hours_minutes, hours, 0)
                                }
                            } else {
                                context.getString(R.string.minutes_only, selectedMinutes)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = context.getString(R.string.three_hours),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentTimerMinutes > 0) {
                    TextButton(onClick = {
                        onTimerSet(0)
                        onDismiss()
                    }) {
                        Text(context.getString(R.string.cancel_countdown))
                    }
                }
                TextButton(onClick = {
                    onTimerSet(selectedMinutes)
                }) {
                    Text(context.getString(R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

/**
 * 音频可视化器组件（三条竖线动画）
 * 参考OpenTune项目的实现风格，符合MD3规范
 */
@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 3
) {
    // 使用Animatable来更好地控制动画状态
    val bar1Height = remember { Animatable(0.25f) }
    val bar2Height = remember { Animatable(0.25f) }
    val bar3Height = remember { Animatable(0.25f) }
    
    // 当isPlaying变化时，启动或停止动画
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // 启动三个独立的动画，使用不同的延迟和持续时间
            launch {
                bar1Height.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                delay(75)
                bar2Height.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(450, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            launch {
                delay(150)
                bar3Height.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        } else {
            // 停止时重置
            bar1Height.animateTo(0.25f, animationSpec = tween(200, easing = EaseInOut))
            bar2Height.animateTo(0.25f, animationSpec = tween(200, easing = EaseInOut))
            bar3Height.animateTo(0.25f, animationSpec = tween(200, easing = EaseInOut))
        }
    }
    
    Canvas(modifier = modifier) {
        val totalBars = barCount
        val barSpacing = 3.dp.toPx() // 竖线之间的间距
        val barWidth = (size.width - barSpacing * (totalBars - 1)) / totalBars
        val minHeight = size.height * 0.25f
        val maxHeight = size.height
        
        // 绘制三根竖线，使用圆角矩形
        val heights = listOf(bar1Height.value, bar2Height.value, bar3Height.value)
        val cornerRadius = barWidth / 2 // 完全圆角
        
        for (i in 0 until barCount) {
            val x = i * (barWidth + barSpacing) + barWidth / 2
            val heightRatio = heights[i].coerceIn(0.25f, 1f)
            val barHeight = minHeight + (maxHeight - minHeight) * heightRatio
            val topY = size.height - barHeight
            
            // 绘制圆角矩形
            drawRoundRect(
                color = color,
                topLeft = Offset(x - barWidth / 2, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

/**
 * 空状态动画组件
 * 使用主题色系的多个深浅版本，保持动画的层次感和可识别性
 */
@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 240.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    // 通过检查background颜色的亮度来判断是否是深色模式
    // 这样可以确保与用户设置的主题一致
    val backgroundHct = colorScheme.background.toHct()
    val isDark = backgroundHct.tone < 50.0
    
    // 获取主色调的HCT值
    val primaryHct = colorScheme.primary.toHct()
    
    // 基于主题色生成不同深浅的颜色，用于替换原动画的不同部分
    // 深色模式下：前景元素需要相对协调，不要太亮（避免米黄色/beige感）
    // 浅色模式下：前景元素使用中等亮度；背景使用非常浅的色调
    
    // 深色（Tone较低）- 用于较暗的前景元素（如鞋子、阴影等）
    // 降低 Chroma 和缩小 Tone 差异，让颜色更柔和
    val darkColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(35.0), if (isDark) 55.0 else 45.0).toInt())
    
    // 中间色（Tone中等）- 用于主要的前景元素（人物身体、衣服、帽子、头发）
    val mediumColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(32.0), if (isDark) 62.0 else 65.0).toInt())
    
    // 浅色（Tone较高）- 用于高光或强调的前景元素
    val lightColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(30.0), if (isDark) 68.0 else 75.0).toInt())
    
    // 背景色 - 深色模式下使用最暗的主题色（形成清晰分层），浅色模式下使用非常浅的主题色
    val backgroundColor = if (isDark) {
        // 深色模式：背景使用最暗的主题色，作为分层的基础
        // Tone 25-30，比darkColor更暗，形成清晰的分层：background(25) < dark(55) < medium(70) < light(85)
        Color(
            Hct.from(
                primaryHct.hue,
                primaryHct.chroma.coerceAtMost(35.0), // 保持一定饱和度以显示主题色特征
                28.0 // 较暗的亮度，作为分层的基础
            ).toInt()
        )
    } else {
        // 浅色模式：背景使用非常浅的主题色
        Color(
            Hct.from(
                primaryHct.hue,
                primaryHct.chroma.coerceAtMost(20.0), // 降低饱和度，使背景更柔和
                96.0 // 非常高的亮度，确保背景很浅很浅
            ).toInt()
        )
    }
    
    // 使用secondary作为辅助色（用于某些特定元素），降低饱和度让它更柔和
    val secondaryHct = colorScheme.secondary.toHct()
    val secondaryColor = Color(Hct.from(secondaryHct.hue, secondaryHct.chroma.coerceAtMost(28.0), secondaryHct.tone).toInt())
    
    // 灰色系颜色 - 用于原动画中的灰色元素，保持无饱和度
    val darkGrayColor = if (isDark) {
        colorScheme.onSurface.copy(alpha = 0.87f)
    } else {
        colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    val mediumGrayColor = if (isDark) {
        colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    
    val lightGrayColor = if (isDark) {
        colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    // 描边色 - 深色模式下使用更亮的颜色以确保可见性
    val strokeColor = if (isDark) {
        // 深色模式：使用onSurface或更亮的outline确保描边清晰可见
        colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        colorScheme.outline
    }
    
    AndroidView(
        factory = { ctx ->
            LottieAnimationView(ctx).apply {
                setAnimation(R.raw.empty_state)
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        },
        update = { view ->
            // 使用自定义的LottieValueCallback来根据原颜色的亮度动态映射到主题色
            // 这样可以保持原动画的颜色层次感
            view.addValueCallback(
                KeyPath("**"),
                LottieProperty.COLOR,
                ThemeColorCallback(
                    darkColor = darkColor.toArgb(),
                    mediumColor = mediumColor.toArgb(),
                    lightColor = lightColor.toArgb(),
                    secondaryColor = secondaryColor.toArgb(),
                    backgroundColor = backgroundColor.toArgb(),
                    darkGrayColor = darkGrayColor.toArgb(),
                    mediumGrayColor = mediumGrayColor.toArgb(),
                    lightGrayColor = lightGrayColor.toArgb(),
                    primaryHct = primaryHct,
                    isDarkMode = isDark
                )
            )
            
            // 设置描边色（确保正确处理带alpha的颜色）
            val strokeArgb = if (strokeColor.alpha < 1.0f) {
                // 如果有alpha通道，需要手动组合
                val alpha = (strokeColor.alpha * 255).toInt()
                val rgb = strokeColor.toArgb() and 0xFFFFFF
                (alpha shl 24) or rgb
            } else {
                strokeColor.toArgb()
            }
            view.addValueCallback(
                KeyPath("**"),
                LottieProperty.STROKE_COLOR,
                LottieValueCallback(strokeArgb)
            )
            
            view.invalidate()
        },
        modifier = modifier.size(size)
    )
}

