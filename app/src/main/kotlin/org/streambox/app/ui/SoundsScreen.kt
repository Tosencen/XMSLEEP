package org.streambox.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import org.streambox.app.R
import org.streambox.app.audio.AudioManager
import org.streambox.app.timer.TimerManager
import androidx.compose.runtime.DisposableEffect

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
    darkMode: org.streambox.app.DarkModeOption = org.streambox.app.DarkModeOption.AUTO,
    onDarkModeChange: (org.streambox.app.DarkModeOption) -> Unit = {}
) {
    val context = LocalContext.current
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
    
    // 6个声音模块的数据
    val soundItems = remember(colorScheme) {
        listOf(
            SoundItem(
                AudioManager.Sound.RAIN,
                "雨声",
                R.raw.rain_animation_optimized,
                colorScheme.primary
            ),
            SoundItem(
                AudioManager.Sound.CAMPFIRE,
                "篝火声",
                R.raw.gouhuo_animation,
                colorScheme.error
            ),
            SoundItem(
                AudioManager.Sound.THUNDER,
                "雷声",
                R.raw.dalei_animation,
                colorScheme.secondary
            ),
            SoundItem(
                AudioManager.Sound.CAT_PURRING,
                "猫咪呼噜",
                R.raw.cat,
                colorScheme.tertiary
            ),
            SoundItem(
                AudioManager.Sound.BIRD_CHIRPING,
                "鸟鸣声",
                R.raw.bird,
                colorScheme.primaryContainer
            ),
            SoundItem(
                AudioManager.Sound.NIGHT_INSECTS,
                "夜虫声",
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
                    android.widget.Toast.makeText(context, "倒计时结束，已停止播放", android.widget.Toast.LENGTH_SHORT).show()
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

    // Tab状态
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("内置", "收藏", "网络")

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部标题和深色模式切换按钮（都在左边）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
                org.streambox.app.DarkModeOption.LIGHT -> false
                org.streambox.app.DarkModeOption.DARK -> true
                org.streambox.app.DarkModeOption.AUTO -> isSystemInDarkTheme()
            }
            
            Surface(
                onClick = {
                    // 在浅色和深色之间切换（跳过AUTO）
                    val newMode = if (isDarkMode) {
                        org.streambox.app.DarkModeOption.LIGHT
                    } else {
                        org.streambox.app.DarkModeOption.DARK
                    }
                    onDarkModeChange(newMode)
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "切换到浅色模式" else "切换到深色模式",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // 顶部按钮式Tab切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                // 为每个tab选择合适的图标
                val icon = when (index) {
                    0 -> Icons.Default.GraphicEq  // 内置（白噪音）
                    1 -> Icons.Default.Star      // 收藏
                    2 -> Icons.Default.Cloud     // 网络
                    else -> Icons.Default.GraphicEq  // 默认使用GraphicEq
                }
                
                FilledTonalButton(
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isSelected) {
                            colorScheme.primaryContainer
                        } else {
                            colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (isSelected) {
                            colorScheme.onPrimaryContainer
                        } else {
                            colorScheme.onSurface
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        
        // Tab内容（支持左右滑动切换）
        var totalDragAmount by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var accumulatedDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 手势结束时判断是否切换
                            val threshold = size.width * 0.25f
                            when {
                                accumulatedDrag > threshold && selectedTabIndex > 0 -> {
                                    selectedTabIndex--
                                }
                                accumulatedDrag < -threshold && selectedTabIndex < tabs.size - 1 -> {
                                    selectedTabIndex++
                                }
                            }
                            accumulatedDrag = 0f
                            totalDragAmount = 0f
                        }
                    ) { change, dragAmount ->
                        accumulatedDrag += dragAmount
                        totalDragAmount = accumulatedDrag
                    }
                }
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // 内置页面（现有的声音列表）
                    var columnsCount by remember { mutableIntStateOf(2) } // 2列或3列
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 标题和布局切换按钮（独立一行）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "白噪音卡片",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { 
                                    columnsCount = if (columnsCount == 2) 3 else 2
                                }
                            ) {
                                Icon(
                                    imageVector = if (columnsCount == 2) Icons.Default.GridView else Icons.Default.ViewAgenda,
                                    contentDescription = if (columnsCount == 2) "切换为3列" else "切换为2列",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        BuiltInSoundsContent(
                            soundItems = soundItems,
                            playingStates = playingStates,
                            audioManager = audioManager,
                            context = context,
                            hideAnimation = hideAnimation,
                            columnsCount = columnsCount
                        )
                    }
                }
                1 -> {
                    // 收藏页面
                    FavoriteSoundsContent()
                }
                2 -> {
                    // 线上页面
                    OnlineSoundsContent()
                }
            }
            
            // 倒计时FAB（所有tab都显示）
            TimerFAB(
                isTimerActive = isTimerActive,
                timeLeftMillis = timeLeftMillis,
                onClick = { showTimerDialog = true },
                enabled = hasPlayingSound || isTimerActive,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
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
                    android.widget.Toast.makeText(context, "已设置${minutes}分钟倒计时", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    timerManager.cancelTimer()
                    android.widget.Toast.makeText(context, "已取消倒计时", android.widget.Toast.LENGTH_SHORT).show()
                }
                showTimerDialog = false
            },
            currentTimerMinutes = if (isTimerActive) timerManager.getCurrentTimerMinutes() else 0
        )
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
    columnsCount: Int = 2
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(soundItems) { item ->
            var showVolumeDialog by remember { mutableStateOf(false) }
            
            SoundCard(
                item = item,
                isPlaying = playingStates[item.sound] ?: false,
                hideAnimation = hideAnimation,
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
                onVolumeClick = { showVolumeDialog = true }
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

/**
 * 收藏声音内容
 */
@Composable
private fun FavoriteSoundsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "暂无收藏",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "收藏的声音将显示在这里",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 线上声音内容
 */
@Composable
private fun OnlineSoundsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "暂无线上内容",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "线上声音将显示在这里",
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
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    // 获取当前主题颜色（在Composable上下文中）
    val currentPrimaryColor = MaterialTheme.colorScheme.primary
    val currentOutlineColor = MaterialTheme.colorScheme.outline
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (hideAnimation) 100.dp else 140.dp)
            .clickable { onToggle(item.sound) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hideAnimation) {
                // 隐藏动画时的布局：标题和音量图标水平排列在中间
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 标题（左侧）
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(alpha)
                    )
                    
                    // 音量图标（右侧，只在播放时显示）
                    if (isPlaying) {
                        IconButton(
                            onClick = onVolumeClick,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "调节音量",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                    
                    // 标题（左上角）
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .alpha(alpha)
                    )
                    
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
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "调节音量",
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
 * 倒计时FAB组件
 * 完全匹配首页FAB的样式：使用Material3默认值，不自定义任何参数
 */
@Composable
private fun TimerFAB(
    isTimerActive: Boolean,
    timeLeftMillis: Long,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // 使用key确保当timeLeftMillis变化时强制重组
    key(timeLeftMillis, isTimerActive) {
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = onClick
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "设置倒计时"
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
    var volume by remember { mutableStateOf(currentVolume) }
    
    val soundName = when (sound) {
        AudioManager.Sound.RAIN -> "雨声"
        AudioManager.Sound.CAMPFIRE -> "篝火声"
        AudioManager.Sound.THUNDER -> "雷声"
        AudioManager.Sound.CAT_PURRING -> "猫咪呼噜"
        AudioManager.Sound.BIRD_CHIRPING -> "鸟鸣声"
        AudioManager.Sound.NIGHT_INSECTS -> "夜虫声"
        else -> "声音"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调节音量 - $soundName") },
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
                Text("确定")
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
    var selectedMinutes by remember { mutableStateOf(if (currentTimerMinutes > 0) currentTimerMinutes else 30) }
    
    // 预设时间选项（分钟）
    val presetMinutes = listOf(15, 30, 45, 60, 90, 120)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置倒计时") },
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
                        "${hours}小时${if (mins > 0) " ${mins}分钟" else ""}"
                    } else {
                        "${mins}分钟"
                    }
                    Text(
                        text = "当前倒计时: $statusText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 预设时间快速选择
                Text(
                    "快速选择",
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
                                        Text("${mins}分钟")
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
                            text = "5分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (selectedMinutes >= 60) {
                                "${selectedMinutes / 60}小时${if (selectedMinutes % 60 > 0) " ${selectedMinutes % 60}分钟" else ""}"
                            } else {
                                "${selectedMinutes}分钟"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "3小时",
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
                        Text("取消倒计时")
                    }
                }
                TextButton(onClick = {
                    onTimerSet(selectedMinutes)
                }) {
                    Text("确定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

