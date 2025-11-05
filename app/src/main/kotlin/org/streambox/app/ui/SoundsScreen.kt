package org.streambox.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onDarkModeChange: (org.streambox.app.DarkModeOption) -> Unit = {},
    columnsCount: Int = 2,
    onColumnsCountChange: (Int) -> Unit = {},
    pinnedSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>
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
                "猫呼噜",
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
    
    // 默认区域编辑模式状态（提升到SoundsScreen级别，以便在切换tab时自动复位）
    var isDefaultAreaEditMode by remember { mutableStateOf(false) }
    
    // 批量选择模式状态（用于批量设置默认）
    var isBatchSelectMode by remember { mutableStateOf(false) }
    var selectedSoundsForBatch by remember { mutableStateOf(mutableSetOf<AudioManager.Sound>()) }
    
    // 切换tab时自动退出编辑模式和批量选择模式
    LaunchedEffect(selectedTabIndex) {
        isDefaultAreaEditMode = false
        isBatchSelectMode = false
        selectedSoundsForBatch.clear()
    }

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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "切换到浅色模式" else "切换到深色模式",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
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
                    onClick = { 
                        // 切换tab时自动退出编辑模式（LaunchedEffect已经处理，这里确保立即生效）
                        isDefaultAreaEditMode = false
                        selectedTabIndex = index 
                    },
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
                    
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 默认区域（位于顶部tab和白噪音卡片标题之间，只在有内容时显示）
                        if (defaultItems.isNotEmpty()) {
                            DefaultArea(
                                soundItems = soundItems,
                                pinnedSounds = pinnedSounds,
                                favoriteSounds = favoriteSounds,
                                playingStates = playingStates,
                                audioManager = audioManager,
                                context = context,
                                isEditMode = isDefaultAreaEditMode,
                                onEditModeChange = { isDefaultAreaEditMode = it },
                                onPinnedChange = { sound, isPinned ->
                                    val currentSet = pinnedSounds.value.toMutableSet()
                                    if (isPinned) {
                                        // 检查是否已达到最大数量（3个）
                                        if (currentSet.size >= 3) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "默认播放区域最多只能添加3个声音",
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
                                }
                            )
                        }
                        
                        // 标题和布局切换按钮（独立一行）
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
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
                                            contentDescription = "取消批量选择",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "已选择（${selectedSoundsForBatch.size} 个）",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "最多可选3个到默认区域",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "白噪音卡片",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
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
                                            contentDescription = "批量选择默认",
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
                                                    "默认播放区域最多只能添加3个声音",
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
                                                    "已更新默认播放区域",
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
                                                text = "完成",
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
                                        contentDescription = if (columnsCount == 2) "切换为3列" else "切换为2列",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
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
                            onSoundBatchSelect = { sound ->
                                val newSet = selectedSoundsForBatch.toMutableSet()
                                if (newSet.contains(sound)) {
                                    newSet.remove(sound)
                                } else {
                                    newSet.add(sound)
                                }
                                selectedSoundsForBatch = newSet
                            },
                            onEditModeReset = { isDefaultAreaEditMode = false },
                            onPinnedChange = { sound, isPinned ->
                                val currentSet = pinnedSounds.value.toMutableSet()
                                if (isPinned) {
                                    // 检查是否已达到最大数量（3个）
                                    if (currentSet.size >= 3) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "默认播放区域最多只能添加3个声音",
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
                1 -> {
                    // 收藏页面
                    FavoriteSoundsContent(
                        soundItems = soundItems,
                        playingStates = playingStates,
                        audioManager = audioManager,
                        context = context,
                        hideAnimation = hideAnimation,
                        columnsCount = columnsCount,
                        pinnedSounds = pinnedSounds,
                        favoriteSounds = favoriteSounds,
                        onPinnedChange = { sound, isPinned ->
                            val currentSet = pinnedSounds.value.toMutableSet()
                            if (isPinned) {
                                // 检查是否已达到最大数量（3个）
                                if (currentSet.size >= 3) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "默认播放区域最多只能添加3个声音",
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
                        }
                    )
                }
                2 -> {
                    // 线上页面
                    OnlineSoundsContent()
                }
            }
            
            // 默认播放区域是否有声音
            val defaultAreaHasSounds = pinnedSounds.value.isNotEmpty()
            
            // 实时检测默认播放区域的播放状态
            var defaultAreaSoundsPlaying by remember { mutableStateOf(false) }
            LaunchedEffect(pinnedSounds.value, Unit) {
                while (true) {
                    defaultAreaSoundsPlaying = pinnedSounds.value.any { audioManager.isPlayingSound(it) }
                    kotlinx.coroutines.delay(300) // 每300ms检查一次
                }
            }
            
            // 播放按钮FAB（放在倒计时按钮上方，符合MD3规范）
            if (defaultAreaHasSounds) {
                FloatingActionButton(
                    onClick = {
                        if (defaultAreaSoundsPlaying) {
                            // 暂停所有默认播放区域的声音
                            pinnedSounds.value.forEach { sound ->
                                if (audioManager.isPlayingSound(sound)) {
                                    audioManager.pauseSound(sound)
                                }
                            }
                        } else {
                            // 播放所有默认播放区域的声音
                            pinnedSounds.value.forEach { sound ->
                                if (!audioManager.isPlayingSound(sound)) {
                                    audioManager.playSound(context, sound)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 100.dp, end = 16.dp) // 增大与倒计时按钮的间距
                        .size(67.2.dp), // 放大20%（56dp * 1.2 = 67.2dp）
                    containerColor = MaterialTheme.colorScheme.primary, // 使用强调主题色
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (defaultAreaSoundsPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (defaultAreaSoundsPlaying) "暂停" else "播放",
                        modifier = Modifier.size(28.8.dp) // 图标也放大20%（24dp * 1.2 = 28.8dp）
                    )
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
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit
) {
    // 实时获取默认卡片列表（使用derivedStateOf确保状态变化时触发重组）
    val defaultItems = remember {
        derivedStateOf {
            soundItems.filter { pinnedSounds.value.contains(it.sound) }
        }
    }.value
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题和编辑按钮（一行）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "默认播放区域",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "点击播放按钮可一键播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 编辑按钮（默认只显示笔图标，编辑模式显示完成+图标）
            if (isEditMode) {
                // 编辑模式下显示"完成"文字+图标
                Surface(
                    onClick = { onEditModeChange(!isEditMode) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(40.dp) // 固定高度与IconButton一致
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "完成",
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
                    onClick = { onEditModeChange(!isEditMode) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 默认区域容器
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (defaultItems.isEmpty()) {
                // 没有默认卡片时，显示占位区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateAnimation(size = 60.dp)
                }
            } else {
                // 有默认卡片时，显示卡片（固定3列，只显示标题和声音图标）
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                }
            }
        }
    }
}

/**
 * 默认卡片组件（简化版，只显示标题和声音图标）
 */
@Composable
private fun DefaultCard(
    item: SoundItem,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isEditMode: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onRemove: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                        contentDescription = "移除",
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
                EmptyStateAnimation(size = 200.dp)
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
    } else {
        // 有收藏时显示卡片列表
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsCount),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
    columnsCount: Int = 2,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    isBatchSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 标题弹窗状态
    var showTitleMenu by remember { mutableStateOf(false) }
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
            // 批量选择模式下的选择框（右上角）
            if (isBatchSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "已选中" else "未选中",
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
                        // 标题（左上角，可点击）
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            var expanded by remember { mutableStateOf(false) }
                            
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(enabled = !isBatchSelectMode) { expanded = true }
                                    .alpha(alpha)
                            )
                            
                            // 标题弹窗
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
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
                                                text = if (isPinned) "取消默认" else "默认",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onPinnedChange(!isPinned)
                                        expanded = false
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
                                                text = if (isFavorite) "取消收藏" else "收藏",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onFavoriteChange(!isFavorite)
                                        expanded = false
                                    }
                                )
                            }
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
                                    .offset(y = 12.dp) // 向下偏移12dp，距离底部28dp
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "调节音量",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        // 2列时：标题在左上角，音量图标在右下角
                        // 标题（左上角，可点击）
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            var expanded by remember { mutableStateOf(false) }
                            
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable(enabled = !isBatchSelectMode) { expanded = true }
                                    .alpha(alpha)
                            )
                            
                            // 标题弹窗
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
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
                                                text = if (isPinned) "取消默认" else "默认",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onPinnedChange(!isPinned)
                                        expanded = false
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
                                                text = if (isFavorite) "取消收藏" else "收藏",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        onFavoriteChange(!isFavorite)
                                        expanded = false
                                    }
                                )
                            }
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
                                    .offset(y = 12.dp) // 向下偏移12dp，距离底部28dp
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "调节音量",
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
                    
                    // 标题（左上角，可点击）
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { expanded = true }
                                .alpha(alpha)
                        )
                        
                        // 标题弹窗
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
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
                                            text = if (isPinned) "取消默认" else "默认",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    onPinnedChange(!isPinned)
                                    expanded = false
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
                                            text = if (isFavorite) "取消收藏" else "收藏",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    onFavoriteChange(!isFavorite)
                                    expanded = false
                                }
                            )
                        }
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
                            .padding(8.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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

/**
 * 音频可视化器组件（三条竖线动画）
 * 参考OpenTune项目的实现风格，符合MD3规范
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
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
 */
@Composable
private fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 200.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val outlineColor = colorScheme.outline
    
    AndroidView(
        factory = { ctx ->
            LottieAnimationView(ctx).apply {
                setAnimation(R.raw.empty_state)
                repeatCount = LottieDrawable.INFINITE
                playAnimation()
            }
        },
        update = { view ->
            // 只设置描边色，使用原始动画文件的填充颜色
            val outlineArgb = outlineColor.toArgb()
            view.addValueCallback(
                KeyPath("**"),
                LottieProperty.STROKE_COLOR,
                LottieValueCallback(outlineArgb)
            )
            
            view.invalidate()
        },
        modifier = modifier.size(size)
    )
}

