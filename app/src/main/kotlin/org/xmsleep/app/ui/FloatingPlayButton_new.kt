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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.model.SoundMetadata
import kotlin.math.roundToInt

/**
 * 全局浮动播放按钮组件 - 重构版
 * 
 * 功能特性：
 * - 固定在屏幕右侧中央
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
    
    // 定时检查播放状态
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
            
            allPlayingSounds = localPlaying + remotePlaying
            
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
    val itemHeight = 72.dp           // 每个音频项的高度
    val headerHeight = 48.dp         // 标题栏高度
    val calculatedHeight = headerHeight + (itemHeight * playingCount.coerceAtMost(5))
    val contentHeight = calculatedHeight.coerceAtMost(maxHeight)
    
    // 动画值
    val width by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "width"
    )
    
    val height by animateDpAsState(
        targetValue = if (isExpanded) contentHeight else buttonHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
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
    
    // 颜色指示：根据播放数量
    val indicatorColor = when {
        playingCount == 0 -> MaterialTheme.colorScheme.outline
        playingCount <= 2 -> MaterialTheme.colorScheme.primary
        playingCount <= 4 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    // 长按检测颜色
    val backgroundColor by animateColorAsState(
        targetValue = if (isLongPressing) {
            MaterialTheme.colorScheme.error
        } else {
            indicatorColor
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )
    
    // 固定位置：屏幕右侧中央
    val fixedX = screenWidth - width
    val fixedY = (screenHeight - height) / 2
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // 确保在最上层
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(fixedX.roundToPx(), fixedY.roundToPx()) }
                .width(width)
                .height(height)
                .shadow(
                    elevation = if (isExpanded) 12.dp else 8.dp,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(
                    color = backgroundColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        bottomStart = 20.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // 点击任意位置（除了按钮）收起
                            if (isExpanded) {
                                isExpanded = false
                            }
                        },
                        onLongPress = {
                            // 长按触发全部停止
                            if (!isExpanded) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isLongPressing = true
                                scope.launch {
                                    delay(800) // 显示提示
                                    if (isLongPressing) {
                                        showStopAllDialog = true
                                        isLongPressing = false
                                    }
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
            if (isExpanded) {
                // 展开状态：显示播放列表
                ExpandedPlayingList(
                    playingSounds = allPlayingSounds,
                    audioManager = audioManager,
                    onCollapse = { isExpanded = false },
                    onStopAll = { showStopAllDialog = true }
                )
            } else {
                // 收起状态：只显示箭头和角标
                CollapsedArrowButton(
                    playingCount = playingCount,
                    breatheScale = breatheScale,
                    arrowRotation = arrowRotation,
                    isLongPressing = isLongPressing,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isExpanded = true
                    }
                )
            }
        }
    }
    
    // 全部停止确认对话框
    if (showStopAllDialog) {
        AlertDialog(
            onDismissRequest = { showStopAllDialog = false },
            icon = { Icon(Icons.Default.Stop, contentDescription = null) },
            title = { Text("停止所有播放") },
            text = { Text("确定要停止所有正在播放的音频吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        audioManager.stopAllSounds()
                        showStopAllDialog = false
                        isExpanded = false
                    }
                ) {
                    Text("停止全部")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopAllDialog = false }) {
                    Text("取消")
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
    breatheScale: Float,
    arrowRotation: Float,
    isLongPressing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 呼吸动画应用于箭头
        Box(
            modifier = Modifier.scale(if (isLongPressing) 1.2f else breatheScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "展开播放列表",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            
            // 数量角标
            if (playingCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Text(
                        text = playingCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 长按提示
        if (isLongPressing) {
            Text(
                text = "松手全部停止",
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
    onStopAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 收起按钮
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "收起",
                    tint = Color.White
                )
            }
            
            // 标题
            Text(
                text = "正在播放 (${ playingSounds.size})",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            // 全部停止按钮
            IconButton(
                onClick = onStopAll,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "全部停止",
                    tint = Color.White
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
                    audioManager = audioManager
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
    audioManager: AudioManager
) {
    // 获取音频音量
    val volume = remember(item.id) {
        when (item) {
            is FloatingPlayItem.Local -> audioManager.getVolume(item.sound)
            is FloatingPlayItem.Remote -> 1.0f // 远程音频默认音量，需要从 AudioManager 获取
        }
    }
    
    var currentVolume by remember { mutableStateOf(volume) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
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
                text = item.name,
                color = Color.White,
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
                        is FloatingPlayItem.Remote -> {
                            // 停止远程音频播放
                            // AudioManager 需要有对应方法
                        }
                    }
                },
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(min = 60.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "停止",
                    fontSize = 11.sp
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
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            
            Slider(
                value = currentVolume,
                onValueChange = { newVolume ->
                    currentVolume = newVolume
                    when (item) {
                        is FloatingPlayItem.Local -> audioManager.setVolume(item.sound, newVolume)
                        is FloatingPlayItem.Remote -> {
                            // 远程音频音量控制，需要实现对应方法
                            // audioManager.setRemoteVolume(item.id, newVolume)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.8f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            Text(
                text = "${(currentVolume * 100).roundToInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
