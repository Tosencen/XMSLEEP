package org.xmsleep.app.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.i18n.LanguageManager
import kotlin.math.roundToInt

/**
 * 全局浮动播放按钮组件
 * 固定在左侧，可展开/收缩，显示正在播放的卡片（本地+远程）
 */
@Composable
fun FloatingPlayButton(
    audioManager: AudioManager,
    onSoundClick: (SoundMetadata) -> Unit,
    selectedTab: Int? = null, // 当前选中的 tab，用于监听 tab 切换
    shouldCollapse: Boolean = false // 外部请求收缩按钮的标志
) {
    // 加载远程声音列表
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val scope = rememberCoroutineScope()
    
    // 停止播放区域高度（默认120dp，按钮在区域内时130dp）
    val defaultStopAreaHeight = 120.dp
    val expandedStopAreaHeight = 130.dp
    
    // 获取当前语言设置
    val currentLanguage = LanguageManager.getCurrentLanguage(context)
    val isEnglish = currentLanguage == LanguageManager.Language.ENGLISH
    val isTraditionalChinese = currentLanguage == LanguageManager.Language.TRADITIONAL_CHINESE
    
    // 检查当前主题是否为深色
    val isDarkTheme = isSystemInDarkTheme()
    
    val resourceManager = remember { AudioResourceManager.getInstance(context) }
    var remoteSounds by remember { mutableStateOf<List<SoundMetadata>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val manifest = resourceManager.loadRemoteManifest()
        if (manifest != null) {
            remoteSounds = manifest.sounds
        } else {
            remoteSounds = resourceManager.getRemoteSounds()
        }
    }
    
    // 按钮尺寸
    val buttonSize = 56.dp
    val expandedCardSpacing = 12.dp // 卡片之间的间距（符合Material Design规范）
    val expandedCardPadding = 12.dp // 卡片区域的内边距
    val cardMinWidth = 48.dp // 卡片最小宽度
    val cardMaxWidth = 120.dp // 卡片最大宽度（避免过长）
    val cardHeight = 40.dp // 卡片高度
    val cardPadding = 12.dp // 卡片内部padding（左右各12dp，符合规范）
    val arrowSpacing = 8.dp // 箭头离按钮的间距
    val arrowIconSize = 24.dp // 箭头图标尺寸
    val outerPaddingLeft = 10.dp // 外层左边距
    val outerPaddingRight = 4.dp // 外层右边距
    val outerPaddingVertical = 10.dp // 外层上下边距
    // 自适应高度：按钮高度(56) + 上下间距(10*2) = 76dp
    val outerBackgroundHeight = buttonSize + outerPaddingVertical * 2
    // 收缩状态下外层背景宽度：按钮(56) + 间距(8) + 箭头(24) + 左padding(10) + 右padding(4) = 102dp
    val outerBackgroundSize = buttonSize + arrowSpacing + arrowIconSize + outerPaddingLeft + outerPaddingRight
    
    // 固定位置：左侧，离底部360dp
    val fixedX = 16.dp
    val fixedY = screenHeight - 360.dp - outerBackgroundHeight
    
    var isExpanded by remember {
        mutableStateOf(false) // 默认未展开状态
    }
    
    // 拖动状态
    var isDragging by remember { mutableStateOf(false) }
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    
    // 按钮是否在停止区域内
    var isButtonInStopArea by remember { mutableStateOf(false) }
    
    // 按钮是否应该跟随红色区域一起消失
    var shouldFadeWithStopArea by remember { mutableStateOf(false) }
    
    // 动态停止区域高度（根据按钮是否在区域内）
    val currentStopAreaHeight by animateDpAsState(
        targetValue = if (isButtonInStopArea) expandedStopAreaHeight else defaultStopAreaHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stopAreaHeight"
    )
    
    // 动态停止区域颜色（根据主题和按钮是否在区域内）
    val stopAreaColor by animateColorAsState(
        targetValue = if (isButtonInStopArea) {
            // 按钮在区域内时，使用更深的红色
            if (isDarkTheme) {
                // 深色主题：使用稍深的红色（亮度变化小）
                Color(0xFF6B1A1A) // 深红色，但不要太暗
            } else {
                // 浅色主题：使用更深的红色（更饱和）
                Color(0xFFD32F2F) // Material Design 的深红色，在浅色背景下更明显
            }
        } else {
            // 默认红色（根据主题调整）
            if (isDarkTheme) {
                // 深色主题：使用较暗的红色（不要太亮）
                Color(0xFF5A1A1A) // 较暗的红色，在深色背景下更协调
            } else {
                // 浅色主题：使用标准 error 颜色
                Color(0xFFBA1A1A) // 使用稍深的红色，确保有变化
            }
        },
        animationSpec = tween(durationMillis = 200),
        label = "stopAreaColor"
    )
    
    // 文字颜色（根据主题适配，确保在红色背景上有良好的对比度）
    val textColor = if (isDarkTheme) {
        // 深色主题：使用白色或浅色文字（在深色红色背景上）
        Color.White
    } else {
        // 浅色主题：使用白色文字（在深色红色背景上）
        Color.White
    }
    
    // 监听 tab 切换，如果按钮是展开状态，自动收缩
    var previousTab by remember { mutableStateOf(selectedTab) }
    LaunchedEffect(selectedTab) {
        if (selectedTab != null && selectedTab != previousTab && isExpanded) {
            isExpanded = false
            PreferencesManager.saveFloatingButtonExpanded(context, false)
        }
        previousTab = selectedTab
    }
    
    // 监听外部请求收缩按钮的标志
    var previousShouldCollapse by remember { mutableStateOf(shouldCollapse) }
    LaunchedEffect(shouldCollapse) {
        if (shouldCollapse && !previousShouldCollapse && isExpanded) {
            isExpanded = false
            PreferencesManager.saveFloatingButtonExpanded(context, false)
        }
        previousShouldCollapse = shouldCollapse
    }
    
    // 获取正在播放的声音（本地+远程，最多3个）
    var allPlayingSounds by remember { mutableStateOf<List<PlayingSoundItem>>(emptyList()) }
    var previousPlayingSounds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 定期检查播放状态
    LaunchedEffect(remoteSounds, Unit) {
        while (true) {
            // 检查本地声音
            val localPlaying = audioManager.getPlayingSounds().map { sound ->
                PlayingSoundItem.Local(sound)
            }
            
            // 检查远程声音
            val remotePlaying = remoteSounds.filter { audioManager.isPlayingRemoteSound(it.id) }.map { sound ->
                PlayingSoundItem.Remote(sound)
            }
            
            // 合并并限制最多3个
            val currentPlaying = (localPlaying + remotePlaying).take(3)
            val currentPlayingIds = currentPlaying.map { item ->
                when (item) {
                    is PlayingSoundItem.Local -> "local_${item.sound.name}"
                    is PlayingSoundItem.Remote -> "remote_${item.sound.id}"
                }
            }.toSet()
            
            // 如果有新的播放声音（之前没有的），且按钮是收缩状态，保持收缩状态
            // 如果按钮是展开状态，不要收缩（用户可能是在添加卡片）
            val hasNewPlaying = currentPlayingIds.any { it !in previousPlayingSounds }
            if (hasNewPlaying && currentPlayingIds.isNotEmpty() && !isExpanded) {
                // 只有在收缩状态时，新播放声音才保持收缩状态
                // 如果按钮是展开状态，不要收缩，让用户继续添加卡片
            }
            
            previousPlayingSounds = currentPlayingIds
            allPlayingSounds = currentPlaying
            delay(300) // 每300ms检查一次
        }
    }
    
    val hasPlayingSounds = allPlayingSounds.isNotEmpty()
    
    // 如果没有播放的声音，隐藏按钮
    if (!hasPlayingSounds) {
        return
    }
    
    // 展开后的内容区域宽度（自适应，由卡片内容决定）
    // 注意：实际宽度会在布局时根据卡片文字内容自动计算
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f) // 确保在最上层
    ) {
        // 底部停止播放区域（拖动时显示）- 先渲染，在按钮下方
        androidx.compose.animation.AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Box(
                modifier = Modifier
                    .zIndex(999f) // 在按钮下方
                    .offset {
                        IntOffset(
                            x = 0,
                            y = with(density) { (screenHeight - currentStopAreaHeight).roundToPx() }
                        )
                    }
                    .fillMaxWidth()
                    .height(currentStopAreaHeight)
                    .background(
                        color = stopAreaColor,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.drag_to_stop_play),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
        }
        
        // 浮动按钮（固定在左侧）- 后渲染，在红色区域上方
        androidx.compose.animation.AnimatedVisibility(
            visible = !shouldFadeWithStopArea || isDragging, // 如果应该跟随红色区域消失，则在红色区域消失时一起消失
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Box(
                modifier = Modifier
                    .zIndex(1001f) // 确保在红色区域上方
                    .offset {
                    IntOffset(
                        x = with(density) { fixedX.roundToPx() } + dragOffsetX.value.toInt(),
                        y = with(density) { fixedY.roundToPx() } + dragOffsetY.value.toInt()
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            isButtonInStopArea = false // 重置状态
                        },
                        onDragEnd = {
                            // 检测是否在停止区域内
                            val currentButtonY = with(density) { fixedY.toPx() } + dragOffsetY.value
                            val stopAreaTopY = with(density) { (screenHeight - currentStopAreaHeight).toPx() }
                            
                            if (currentButtonY >= stopAreaTopY) {
                                // 在停止区域内，停止所有播放并让按钮跟随红色区域一起消失
                                scope.launch {
                                    // 停止所有本地声音
                                    audioManager.getPlayingSounds().forEach { sound ->
                                        audioManager.pauseSound(sound)
                                    }
                                    // 停止所有远程声音
                                    allPlayingSounds.forEach { item ->
                                        when (item) {
                                            is PlayingSoundItem.Remote -> {
                                                onSoundClick(item.sound)
                                            }
                                            else -> {}
                                        }
                                    }
                                    
                                    // 按钮应该跟随红色区域一起消失（保持在当前位置）
                                    shouldFadeWithStopArea = true
                                    
                                    // 立即隐藏红色区域（让按钮跟随一起消失）
                                    isDragging = false
                                    isButtonInStopArea = false
                                    
                                    // 等待渐出动画完成后再重置按钮位置和状态
                                    delay(200) // 等待渐出动画完成
                                    shouldFadeWithStopArea = false
                                    dragOffsetX.snapTo(0f)
                                    dragOffsetY.snapTo(0f)
                                }
                            } else {
                                // 不在停止区域内，正常复位
                                isDragging = false
                                isButtonInStopArea = false
                                scope.launch {
                                    // 放手时复位，使用动画
                                    dragOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            // 拖动时实时更新位置（snapTo 是挂起函数，需要在协程中调用）
                            scope.launch {
                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                                
                                // 实时检测按钮是否在停止区域内（使用当前区域高度）
                                val currentButtonY = with(density) { fixedY.toPx() } + dragOffsetY.value
                                // 使用当前区域高度来检测（考虑按钮已经在区域内时区域会变高）
                                val stopAreaTopY = with(density) { 
                                    val areaHeight = if (isButtonInStopArea) expandedStopAreaHeight else defaultStopAreaHeight
                                    (screenHeight - areaHeight).toPx() 
                                }
                                val newIsInStopArea = currentButtonY >= stopAreaTopY
                                if (newIsInStopArea != isButtonInStopArea) {
                                    isButtonInStopArea = newIsInStopArea
                                }
                            }
                        }
                    )
                }
                .widthIn(
                    min = outerBackgroundSize,
                    max = if (isExpanded && allPlayingSounds.isNotEmpty()) {
                        // 展开状态：不限制最大宽度，完全自适应
                        androidx.compose.ui.unit.Dp.Infinity
                    } else {
                        // 收缩状态：固定宽度
                        outerBackgroundSize
                    }
                )
                .wrapContentWidth(Alignment.Start)
                .height(outerBackgroundHeight)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // 展开状态：整个按钮区域可点击收缩
            // 收缩状态：整个按钮区域可点击展开
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .padding(start = outerPaddingLeft, end = outerPaddingRight, top = outerPaddingVertical, bottom = outerPaddingVertical)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        isExpanded = !isExpanded
                        PreferencesManager.saveFloatingButtonExpanded(context, isExpanded)
                    },
                horizontalArrangement = Arrangement.spacedBy(arrowSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 悬浮按钮内容区域
                Box {
                    // 展开后的卡片区域（使用 AnimatedVisibility 实现展开/收缩动画）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded && allPlayingSounds.isNotEmpty(),
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(buttonSize)
                                .padding(expandedCardPadding),
                            horizontalArrangement = Arrangement.spacedBy(expandedCardSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 播放的卡片（最多3个）
                            allPlayingSounds.forEach { item ->
                                FloatingPlayCard(
                                    item = item,
                                    minWidth = cardMinWidth,
                                    maxWidth = cardMaxWidth,
                                    height = cardHeight,
                                    padding = cardPadding,
                                    onClick = {
                                        when (item) {
                                            is PlayingSoundItem.Local -> {
                                                // 暂停本地声音
                                                scope.launch {
                                                    audioManager.pauseSound(item.sound)
                                                }
                                            }
                                            is PlayingSoundItem.Remote -> {
                                                // 暂停远程声音
                                                onSoundClick(item.sound)
                                            }
                                        }
                                    },
                                    isEnglish = isEnglish,
                                    isTraditionalChinese = isTraditionalChinese
                                )
                            }
                        }
                    }
                    
                    // 收缩状态：只显示按钮（使用 AnimatedVisibility 实现展开/收缩动画）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isExpanded || allPlayingSounds.isEmpty(),
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // 三条竖杠动画
                            AudioVisualizer(
                                modifier = Modifier.size(24.dp),
                                isPlaying = true,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // 右侧箭头图标（竖着展示，点击由父级Row处理）
                Box(
                    modifier = Modifier
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomChevronIcon(
                        isExpanded = isExpanded,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(90f) // 竖着展示（旋转90度）
                    )
                }
            }
        }
        }
    }
}

/**
 * 播放声音项（本地或远程）
 */
private sealed class PlayingSoundItem {
    data class Local(val sound: AudioManager.Sound) : PlayingSoundItem()
    data class Remote(val sound: SoundMetadata) : PlayingSoundItem()
}

/**
 * 浮动播放卡片（展开后显示，自适应大小，只显示文字）
 */
@Composable
private fun FloatingPlayCard(
    item: PlayingSoundItem,
    minWidth: androidx.compose.ui.unit.Dp,
    maxWidth: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    padding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    isEnglish: Boolean,
    isTraditionalChinese: Boolean
) {
    val context = LocalContext.current
    val displayName = when (item) {
        is PlayingSoundItem.Local -> {
            // 获取本地声音的本地化名称
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
        is PlayingSoundItem.Remote -> {
            // 获取远程声音的本地化名称
            when {
                isTraditionalChinese && !item.sound.nameZhTW.isNullOrEmpty() -> item.sound.nameZhTW
                isEnglish && !item.sound.nameEn.isNullOrEmpty() -> item.sound.nameEn
                else -> item.sound.name
            }
        }
    }
    
    Box(
        modifier = Modifier
            .widthIn(min = minWidth, max = maxWidth)
            .height(height)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 只显示文字，自适应宽度
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = padding)
        )
    }
}
