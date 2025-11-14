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
 * - 默认收起状态（只显示箭头图标 40dp）
 * - 点击箭头展开播放列表（280dp）
 * - 显示正在播放的音频及音量控制
 * - 支持单个停止和全部停止
 * - 滑动页面自动收起
 * - 数量角标、颜色指示、微动画
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
}

@Composable
fun FloatingPlayButtonNew(
    audioManager: AudioManager,
    onSoundClick: (SoundMetadata) -> Unit = {},
    selectedTab: Int? = null,
    shouldCollapse: Boolean = false
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
    
    // 长按状态
    var isLongPressing by remember { mutableStateOf(false) }
    
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
            
            // 合并并限制最多10个
            allPlayingSounds = (localPlaying + remotePlaying).take(10)
            
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
    val collapsedWidth = 40.dp      // 收起时的宽度（只显示箭头）
    val expandedWidth = 280.dp       // 展开时的宽度
    val buttonHeight = 80.dp         // 最小高度
    val maxHeight = 400.dp           // 最大高度
    
    // 根据播放数量计算高度
    val itemHeight = 80.dp           // 每个音频项的高度（足够显示名称、按钮和音量滑块）
    val headerHeight = 48.dp         // 标题栏高度
    val minContentHeight = 168.dp    // 单个音频时的最小高度（舒适空间）
    
    val calculatedHeight = if (playingCount == 1) {
        // 单个音频时使用更大的高度
        minContentHeight
    } else {
        // 多个音频时按实际数量计算（最多10个）
        headerHeight + (itemHeight * playingCount.coerceAtMost(10))
    }
    // 增加最大高度限制以容纳更多音频
    val adjustedMaxHeight = (screenHeight * 0.8f).coerceAtMost(700.dp)
    val contentHeight = calculatedHeight.coerceAtMost(adjustedMaxHeight)
    
    // 动画值（减慢50%：降低刚度）
    val width by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "width"
    )
    
    val height by animateDpAsState(
        targetValue = if (isExpanded) contentHeight else buttonHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
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
    val buttonContainerColor by animateColorAsState(
        targetValue = if (isLongPressing) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(200),
        label = "buttonContainerColor"
    )
    
    val buttonContentColor = if (isLongPressing) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExpanded = true
                        },
                        onLongPress = {
                            // 长按触发全部停止
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLongPressing = true
                            scope.launch {
                                delay(800) // 显示提示
                                if (isLongPressing) {
                                    showStopAllDialog = true
                                    isLongPressing = false
                                }
                            }
                        },
                        onPress = {
                            val pressed = tryAwaitRelease()
                            if (!pressed) {
                                isLongPressing = false
                            }
                        }
                    )
                }
            ) {
                // 收起状态：只显示箭头和角标
                CollapsedArrowButton(
                    playingCount = playingCount,
                    arrowRotation = arrowRotation,
                    isLongPressing = isLongPressing,
                    contentColor = buttonContentColor
                )
            }
        }
    }
    
    // 全部停止确认对话框
    if (showStopAllDialog) {
        AlertDialog(
            onDismissRequest = { showStopAllDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.stop_all_confirm_title)) },
            text = { Text(stringResource(R.string.stop_all_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        audioManager.stopAllSounds()
                        showStopAllDialog = false
                        isExpanded = false
                    }
                ) {
                    Text(stringResource(R.string.stop_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * 收起状态的箭头按钮
 * 只显示箭头图标和数量角标
 */
@Composable
private fun CollapsedArrowButton(
    playingCount: Int,
    arrowRotation: Float,
    isLongPressing: Boolean,
    contentColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 箭头图标（不再应用呼吸动画）
        Box(
            modifier = Modifier.scale(if (isLongPressing) 1.2f else 1.0f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.expand),
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            
            // 移除数量角标（红色圆）
        }
        
        // 长按提示
        if (isLongPressing) {
            Text(
                text = stringResource(R.string.stop_all),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 20.dp)
            )
        }
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
    contentColor: Color,
    containerColor: Color // 展开容器的背景色
) {
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
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
        
        // 播放列表（可滚动）
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playingSounds, key = { it.id }) { item ->
                FloatingPlayItemView(
                    item = item,
                    audioManager = audioManager,
                    contentColor = contentColor,
                    containerColor = containerColor
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
    containerColor: Color // 展开容器的背景色
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
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
                AudioManager.Sound.THUNDER_NEW -> context.getString(R.string.sound_thunder_new)
                AudioManager.Sound.CLOCK -> context.getString(R.string.sound_clock)
                AudioManager.Sound.FOREST_BIRDS -> context.getString(R.string.sound_forest_birds)
                AudioManager.Sound.DRIFTING -> context.getString(R.string.sound_drifting)
                AudioManager.Sound.CAMPFIRE_NEW -> context.getString(R.string.sound_campfire_new)
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
    }
    // 获取音频音量
    val volume = remember(item.id) {
        when (item) {
            is FloatingPlayItem.Local -> audioManager.getVolume(item.sound)
            is FloatingPlayItem.Remote -> audioManager.getRemoteVolume(item.id)
        }
    }
    
    var currentVolume by remember { mutableStateOf(volume) }
    
    // 音频卡片背景色：比展开容器深5%
    val cardBackgroundColor = containerColor.copy(
        red = (containerColor.red * 0.95f).coerceAtLeast(0f),
        green = (containerColor.green * 0.95f).coerceAtLeast(0f),
        blue = (containerColor.blue * 0.95f).coerceAtLeast(0f)
    )
    
    // 音频卡片内容颜色：确保对比度
    val cardContentColor = if (isDarkTheme) {
        // 深色模式：使用亮色文字
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        // 浅色模式：使用深色文字
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            FilledTonalButton(
                onClick = {
                    when (item) {
                        is FloatingPlayItem.Local -> audioManager.pauseSound(item.sound)
                        is FloatingPlayItem.Remote -> audioManager.pauseRemoteSound(item.id)
                    }
                },
                modifier = Modifier
                    .size(28.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = containerColor,
                    contentColor = if (isDarkTheme) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 音量滑块
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    currentVolume = newVolume
                    when (item) {
                        is FloatingPlayItem.Local -> audioManager.setVolume(item.sound, newVolume)
                        is FloatingPlayItem.Remote -> audioManager.setRemoteVolume(item.id, newVolume)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = contentColor,
                    activeTrackColor = contentColor.copy(alpha = 0.8f),
                    inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                )
            )
            
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
