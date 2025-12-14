package org.xmsleep.app.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.model.SoundMetadata
import kotlin.math.roundToInt

/**
 * 全局浮动播放按钮组件 - 重构版
 * 
 * 功能特性：
 * - 固定在屏幕左侧中央
 * - 默认收起状态（20dp 窄条，无箭头，有呼吸动画）
 * - 点击展开播放列表（280dp）
 * - 展开时显示所有正在播放的音频，支持滚动
 * - 显示正在播放的音频及音量控制
 * - 支持单个停止和全部停止
 * - 滑动页面或切换 tab 自动收起
 * 
 * @param audioManager 音频管理器
 * @param onSoundClick 点击音频回调
 * @param shouldCollapse 外部触发收起标志（滚动检测）
 */
/**
 * 播放音频项的密封类（新版本）
 */
private sealed interface FloatingPlayItem {
    val id: String
    val name: String
    val isRemote: Boolean
    
    data class Local(val sound: AudioManager.Sound) : FloatingPlayItem {
        override val id: String get() = sound.name
        override val name: String get() = sound.name
        override val isRemote: Boolean get() = false
    }
    
    data class Remote(val sound: SoundMetadata) : FloatingPlayItem {
        override val id: String get() = sound.id
        override val name: String get() = sound.name
        override val isRemote: Boolean get() = true
    }
    
    data class LocalAudio(val audioId: Long, val title: String) : FloatingPlayItem {
        override val id: String get() = "local_audio_$audioId"
        override val name: String get() = title
        override val isRemote: Boolean get() = false
    }
}

@Composable
fun FloatingPlayButtonNew(
    audioManager: AudioManager,
    onSoundClick: (SoundMetadata) -> Unit = {},
    selectedTab: Int? = null,
    shouldCollapse: Boolean = false,
    activePreset: Int = 1, // 当前激活的预设
    onAddToPreset: (localSounds: List<AudioManager.Sound>, remoteSoundIds: List<String>) -> Unit = { _, _ -> }, // 添加到预设的回调
    forceCollapse: Boolean = false // 强制收缩悬浮播放按钮
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    
    // 按钮状态
    var isExpanded by remember { mutableStateOf(false) }
    var showStopAllDialog by remember { mutableStateOf(false) }
    
    // 监听强制收缩参数
    LaunchedEffect(forceCollapse) {
        if (forceCollapse && isExpanded) {
            isExpanded = false
        }
    }
    
    // 加载远程音频列表
    val resourceManager = remember { 
        org.xmsleep.app.audio.AudioResourceManager.getInstance(context) 
    }
    var remoteSounds by remember { mutableStateOf<List<SoundMetadata>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val manifest = resourceManager.loadRemoteManifest()
        if (manifest != null) {
            remoteSounds = manifest.sounds
        } else {
            remoteSounds = resourceManager.getRemoteSounds()
        }
    }
    
    // 获取正在播放的音频列表
    var allPlayingSounds by remember { mutableStateOf<List<FloatingPlayItem>>(emptyList()) }
    
    // 定时检查播放状态（最多支持10个同时播放）
    val localAudioPlayer = remember { org.xmsleep.app.audio.LocalAudioPlayer.getInstance() }
    val playingAudioIds by localAudioPlayer.playingAudioIds.collectAsState()
    
    
    LaunchedEffect(Unit) {
        while (true) {
            // 检查本地声音
            val localPlaying = audioManager.getPlayingSounds().map { sound ->
                FloatingPlayItem.Local(sound)
            }
            
            // 检查远程声音
            val remotePlaying = remoteSounds.filter { 
                audioManager.isPlayingRemoteSound(it.id) 
            }.map { sound ->
                FloatingPlayItem.Remote(sound)
            }
            
            // 检查本地音频文件 - 支持多个音频同时播放
            val localAudioPlaying = if (playingAudioIds.isNotEmpty()) {
                // 为每个正在播放的音频查询标题
                playingAudioIds.mapNotNull { audioId ->
                    val currentTitle = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL)
                            } else {
                                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            }
                            val uri = android.content.ContentUris.withAppendedId(collection, audioId)
                            context.contentResolver.query(
                                uri,
                                arrayOf(android.provider.MediaStore.Audio.Media.DISPLAY_NAME),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val displayNameColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
                                    val displayName = cursor.getString(displayNameColumn)
                                    displayName.substringBeforeLast(".")
                                } else null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    if (currentTitle != null) {
                        FloatingPlayItem.LocalAudio(audioId, currentTitle)
                    } else {
                        null
                    }
                }
            } else {
                emptyList()
            }
            
            // 合并并限制最多10个
            allPlayingSounds = (localPlaying + remotePlaying + localAudioPlaying).take(10)
            
            delay(300) // 每300ms检查一次
        }
    }
    
    val playingCount = allPlayingSounds.size
    
    // 监听外部收起请求（滚动时）
    LaunchedEffect(shouldCollapse) {
        if (shouldCollapse && isExpanded) {
            isExpanded = false
        }
    }
    
    // 监听 tab 切换，自动收起
    var previousTab by remember { mutableStateOf(selectedTab) }
    LaunchedEffect(selectedTab) {
        if (selectedTab != null && selectedTab != previousTab && isExpanded) {
            isExpanded = false
        }
        previousTab = selectedTab
    }
    
    // 如果没有播放，隐藏按钮
    if (playingCount == 0) {
        return
    }
    
    // 尺寸配置
    val collapsedWidth = 20.dp      // 收起时的宽度（窄条，无箭头）
    val expandedWidth = 230.dp       // 展开时的宽度
    val buttonHeight = 80.dp         // 最小高度
    
    // 动态高度计算
    val minHeight = 240.dp           // 最小高度（单个音频时）
    val maxHeight = 460.dp           // 最大高度（多个音频时）
    val itemHeight = 88.dp           // 每个音频项的高度
    val headerHeight = 48.dp         // 标题栏高度
    val itemSpacing = 8.dp           // 音频项之间的间距
    
    // 根据播放数量计算高度
    val calculatedHeight = headerHeight + 4.dp + (itemHeight * playingCount.coerceAtMost(10)) + (itemSpacing * (playingCount - 1).coerceAtLeast(0))
    val contentHeight = calculatedHeight.coerceIn(minHeight, maxHeight)
    
    // 动画值（使用平滑过渡，避免过度反弹）
    val width by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "width"
    )
    
    val height by animateDpAsState(
        targetValue = if (isExpanded) contentHeight else buttonHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "height"
    )
    
    // 箭头旋转动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "arrowRotation"
    )
    
    // 微动画：呼吸效果（收起时）
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )
    
    // 颜色指示：根据播放数量（最多10个）
    val indicatorColor = when {
        playingCount == 0 -> MaterialTheme.colorScheme.outline
        playingCount <= 3 -> MaterialTheme.colorScheme.primary
        playingCount <= 6 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    // 使用 Material3 规范的颜色系统
    val buttonContainerColor = MaterialTheme.colorScheme.primaryContainer
    val buttonContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    
    // 展开模块背景色与箭头相同，但深色模式下稍微浅一点
    val expandedContainerColor = if (isDarkTheme) {
        // 深色模式：在 primaryContainer 基础上提亮一点点
        buttonContainerColor.copy(
            red = (buttonContainerColor.red * 1.15f).coerceAtMost(1f),
            green = (buttonContainerColor.green * 1.15f).coerceAtMost(1f),
            blue = (buttonContainerColor.blue * 1.15f).coerceAtMost(1f)
        )
    } else {
        buttonContainerColor
    }
    val expandedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    
    // 固定位置：屏幕最左侧中央
    val arrowWidth = 40.dp
    val fixedX = 0.dp // 吸附到最左侧边缘
    val fixedY = (screenHeight - height) / 2
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // 确保在最上层
    ) {
        // 展开时：内容在左，箭头在右（各自独立投影）
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .offset { IntOffset(fixedX.roundToPx(), fixedY.roundToPx()) }
                    .height(height),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧内容区域（展开的播放列表，独立投影）
                Box(
                    modifier = Modifier
                        .width(expandedWidth)
                        .fillMaxHeight()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(
                            color = expandedContainerColor,
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 16.dp,
                                bottomEnd = 16.dp
                            )
                        )
                ) {
                    ExpandedPlayingList(
                        playingSounds = allPlayingSounds,
                        audioManager = audioManager,
                        onCollapse = { isExpanded = false },
                        onStopAll = { showStopAllDialog = true },
                        onAddToPreset = onAddToPreset,
                        contentColor = expandedContentColor,
                        containerColor = expandedContainerColor
                    )
                }
                
                // 右侧箭头按钮（只占中间高度 buttonHeight，展开时无投影）
                Box(
                    modifier = Modifier
                        .width(arrowWidth)
                        .height(buttonHeight)
                        .background(
                            color = buttonContainerColor,
                            shape = RoundedCornerShape(
                                topStart = 0.dp,
                                bottomStart = 0.dp,
                                topEnd = 20.dp,
                                bottomEnd = 20.dp
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isExpanded = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.collapse),
                        tint = buttonContentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            // 收起时：只显示箭头（应用呼吸动画到整个容器）
            Box(
                modifier = Modifier
                    .offset { IntOffset(fixedX.roundToPx(), fixedY.roundToPx()) }
                    .scale(breatheScale) // 整个容器应用呼吸效果
                    .width(width)
                    .height(height)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                    .background(
                        color = buttonContainerColor,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            bottomStart = 0.dp,
                            topEnd = 20.dp,
                            bottomEnd = 20.dp
                        )
                    )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = true
                    }
                )
            ) {
                // 收起状态：空的窄条，只有呼吸动画
            }
        }
    }
    
    // 全部停止确认对话框
    if (showStopAllDialog) {
        AlertDialog(
            onDismissRequest = { showStopAllDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(context.getString(R.string.stop_all_confirm_title)) },
            text = { Text(context.getString(R.string.stop_all_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        audioManager.stopAllSounds()
                        showStopAllDialog = false
                        isExpanded = false
                    }
                ) {
                    Text(context.getString(R.string.stop_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopAllDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 展开状态的播放列表
 * 显示所有正在播放的音频及控制项
 */
@Composable
private fun ExpandedPlayingList(
    playingSounds: List<FloatingPlayItem>,
    audioManager: AudioManager,
    onCollapse: () -> Unit,
    onStopAll: () -> Unit,
    onAddToPreset: (localSounds: List<AudioManager.Sound>, remoteSoundIds: List<String>) -> Unit,
    contentColor: Color,
    containerColor: Color // 展开容器的背景色
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
    // 管理可见的播放项目（支持动画移除）
    var visiblePlayingSounds by remember { mutableStateOf(playingSounds) }
    
    // 管理正在移除的项目ID集合（统一动画状态）
    var removingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 当外部播放列表变化时，同步更新可见列表
    LaunchedEffect(playingSounds) {
        visiblePlayingSounds = playingSounds
        removingIds = emptySet() // 重置移除状态
    }
    
    // 监听移除动画完成，真正从可见列表中移除项目
    LaunchedEffect(removingIds) {
        if (removingIds.isNotEmpty()) {
            // 等待动画完成（600ms + 100ms延迟）
            delay(700)
            // 从可见列表中移除已完成动画的项目
            visiblePlayingSounds = visiblePlayingSounds.filter { !removingIds.contains(it.id) }
            // 清空移除集合
            removingIds = emptySet()
        }
    }
    
    // 额外的安全机制：定期检查移除状态，防止卡片卡住
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 每秒检查一次
            if (removingIds.isNotEmpty()) {
                // 如果有卡片长时间处于移除状态，强制清理
                delay(200) // 给最后一次动画机会
                if (removingIds.isNotEmpty()) {
                    visiblePlayingSounds = visiblePlayingSounds.filter { !removingIds.contains(it.id) }
                    removingIds = emptySet()
                }
            }
        }
    }
    
    // 计算按钮背景色：与声音卡片相同（containerColor 的 95% 深度）
    val buttonBackgroundColor = containerColor.copy(
        red = (containerColor.red * 0.95f).coerceAtLeast(0f),
        green = (containerColor.green * 0.95f).coerceAtLeast(0f),
        blue = (containerColor.blue * 0.95f).coerceAtLeast(0f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏（不再显示收起按钮，因为箭头已经在外面了）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 拦截点击，防止穿透 */ }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.playing_title, playingSounds.size),
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                
                // 全部停止按钮（使用 Stop 图标而非 Delete）
                IconButton(
                    onClick = onStopAll,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.stop_all),
                        tint = contentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 播放列表（可滚动）- 底部留出按钮空间（48dp + 8dp间距 = 56dp）
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 56.dp), // 为底部按钮留出空间
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visiblePlayingSounds, key = { it.id }) { item ->
                    FloatingPlayItemView(
                        item = item,
                        audioManager = audioManager,
                        contentColor = contentColor,
                        containerColor = containerColor,
                        isRemoving = removingIds.contains(item.id),
                        onRemove = {
                            // 将项目添加到正在移除的集合中，触发动画
                            removingIds = removingIds + item.id
                        }
                    )
                }
            }
        }
        
        // "添加到预设"按钮（固定在底部）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter),
            color = buttonBackgroundColor, // 使用与声音卡片相同的背景色
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 0.dp // 无投影
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // 分别收集本地声音和远程声音
                        val localSounds = playingSounds.mapNotNull { item ->
                            when (item) {
                                is FloatingPlayItem.Local -> item.sound
                                is FloatingPlayItem.Remote -> null
                                is FloatingPlayItem.LocalAudio -> null
                            }
                        }
                        val remoteSoundIds = playingSounds.mapNotNull { item ->
                            when (item) {
                                is FloatingPlayItem.Local -> null
                                is FloatingPlayItem.Remote -> item.sound.id
                                is FloatingPlayItem.LocalAudio -> null
                            }
                        }
                        
                        if (localSounds.isNotEmpty() || remoteSoundIds.isNotEmpty()) {
                            onAddToPreset(localSounds, remoteSoundIds)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.no_playing_sounds),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_to_preset),
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 单个播放音频项
 * 显示音频名称、音量控制和停止按钮
 */
@Composable
private fun FloatingPlayItemView(
    item: FloatingPlayItem,
    audioManager: AudioManager,
    contentColor: Color,
    containerColor: Color, // 展开容器的背景色
    isRemoving: Boolean = false, // 是否正在移除（动画状态）
    onRemove: () -> Unit = {} // 移除卡片的回调
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val localAudioPlayer = remember { org.xmsleep.app.audio.LocalAudioPlayer.getInstance() }
    
    // 获取当前语言设置
    val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
    val isEnglish = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.ENGLISH
    val isTraditionalChinese = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.TRADITIONAL_CHINESE
    
    // 获取本地化名称
    val displayName = when (item) {
        is FloatingPlayItem.Local -> {
            // 本地声音：使用 getString() 获取本地化名称
            when (item.sound) {
                AudioManager.Sound.UMBRELLA_RAIN -> context.getString(R.string.sound_umbrella_rain)
                AudioManager.Sound.ROWING -> context.getString(R.string.sound_rowing)
                AudioManager.Sound.OFFICE -> context.getString(R.string.sound_office)
                AudioManager.Sound.LIBRARY -> context.getString(R.string.sound_library)
                AudioManager.Sound.HEAVY_RAIN -> context.getString(R.string.sound_heavy_rain)
                AudioManager.Sound.TYPEWRITER -> context.getString(R.string.sound_typewriter)
                AudioManager.Sound.THUNDER -> context.getString(R.string.sound_thunder)
                AudioManager.Sound.CLOCK -> context.getString(R.string.sound_clock)
                AudioManager.Sound.FOREST_BIRDS -> context.getString(R.string.sound_forest_birds)
                AudioManager.Sound.DRIFTING -> context.getString(R.string.sound_drifting)
                AudioManager.Sound.CAMPFIRE -> context.getString(R.string.sound_campfire)
                AudioManager.Sound.WIND -> context.getString(R.string.sound_wind)
                AudioManager.Sound.KEYBOARD -> context.getString(R.string.sound_keyboard)
                AudioManager.Sound.SNOW_WALKING -> context.getString(R.string.sound_snow_walking)
                else -> item.sound.name
            }
        }
        is FloatingPlayItem.Remote -> {
            // 远程声音：根据语言选择字段
            when {
                isTraditionalChinese && !item.sound.nameZhTW.isNullOrEmpty() -> item.sound.nameZhTW
                isEnglish && !item.sound.nameEn.isNullOrEmpty() -> item.sound.nameEn
                else -> item.sound.name
            }
        }
        is FloatingPlayItem.LocalAudio -> item.title
    }
    // 获取音频音量
    val volume = remember(item.id) {
        when (item) {
            is FloatingPlayItem.Local -> audioManager.getVolume(item.sound)
            is FloatingPlayItem.Remote -> audioManager.getRemoteVolume(item.id)
            is FloatingPlayItem.LocalAudio -> localAudioPlayer.getVolume(item.audioId)
        }
    }
    
    var currentVolume by remember { mutableStateOf(volume) }
    
    // 使用外部传入的移除状态来控制动画
    val offsetX by animateFloatAsState(
        targetValue = if (isRemoving) -1000f else 0f, // 往左移动消失
        animationSpec = tween(
            durationMillis = 600, // 统一的动画时长
            easing = FastOutSlowInEasing
        ),
        label = "cardOffset_${item.id}" // 为每个卡片创建唯一的动画标签
    )
    
    // 监听动画完成，当动画结束时调用移除回调
    LaunchedEffect(offsetX, item.id) {
        if (offsetX == -1000f) {
            // 动画完成，延迟一点时间后移除
            delay(100)
            onRemove()
        }
    }
    
    // 音频卡片背景色：比展开容器深5%
    val cardBackgroundColor = containerColor.copy(
        red = (containerColor.red * 0.95f).coerceAtLeast(0f),
        green = (containerColor.green * 0.95f).coerceAtLeast(0f),
        blue = (containerColor.blue * 0.95f).coerceAtLeast(0f)
    )
    
    // 音频卡片内容颜色：统一使用 contentColor (onPrimaryContainer) 保持一致性
    val cardContentColor = contentColor
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.toInt(), 0) }
            .background(
                color = cardBackgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 音频名称
            Text(
                text = displayName,
                color = cardContentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // 停止按钮
            Box(
                modifier = Modifier.size(28.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        // 先停止音频
                        when (item) {
                            is FloatingPlayItem.Local -> audioManager.pauseSound(item.sound)
                            is FloatingPlayItem.Remote -> audioManager.pauseRemoteSound(item.id)
                            is FloatingPlayItem.LocalAudio -> localAudioPlayer.stopAudio(item.audioId)
                        }
                        // 调用外部回调触发移除动画
                        onRemove()
                    },
                    modifier = Modifier.size(28.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 音量滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            Icon(
                imageVector = Icons.Default.VolumeDown,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    currentVolume = newVolume
                    when (item) {
                        is FloatingPlayItem.Local -> audioManager.setVolume(item.sound, newVolume)
                        is FloatingPlayItem.Remote -> audioManager.setRemoteVolume(item.id, newVolume)
                        is FloatingPlayItem.LocalAudio -> localAudioPlayer.setVolume(item.audioId, newVolume)
                    }
                },
                modifier = Modifier.weight(1f), // 自适应宽度
                colors = SliderDefaults.colors(
                    thumbColor = contentColor,
                    activeTrackColor = contentColor.copy(alpha = 0.8f),
                    inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                )
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${(currentVolume * 100).roundToInt()}%",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
