package org.xmsleep.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.Constants
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.utils.Logger

/**
 * 默认区域组件（快捷播放预设栏）
 */
@Composable
internal fun DefaultArea(
    soundItems: List<SoundItem>,
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    playingStates: MutableMap<AudioManager.Sound, Boolean>,
    soundPlayingPreset: MutableMap<AudioManager.Sound, Int>,
    audioManager: AudioManager,
    context: android.content.Context,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    isExpanded: Boolean = true,
    activePreset: Int = 1,
    onActivePresetChange: (Int) -> Unit = {},
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onEnterBatchSelectMode: () -> Unit = {},
    showEditButton: Boolean = true,
    remoteSounds: List<org.xmsleep.app.audio.model.SoundMetadata> = emptyList(),
    remotePinned: MutableSet<String> = mutableSetOf(),
    downloadingSounds: Map<String, Float> = emptyMap(),
    playingRemoteSounds: Set<String> = emptySet(),
    onRemotePinnedChange: (String, Boolean) -> Unit = { _, _ -> },
    onRemoteCardClick: (org.xmsleep.app.audio.model.SoundMetadata) -> Unit = {},
    getSoundDisplayName: (org.xmsleep.app.audio.model.SoundMetadata) -> String = { it.name },
    scope: CoroutineScope = rememberCoroutineScope(),
    resourceManager: org.xmsleep.app.audio.AudioResourceManager = remember { org.xmsleep.app.audio.AudioResourceManager.getInstance(context) },
    presetList: List<org.xmsleep.app.preferences.PresetPrefs.PresetEntry> = emptyList(),
    verticalLayout: Boolean = false,
    onAddPreset: () -> Unit = {},
    onRenamePreset: (Int) -> Unit = {},
    onDeletePreset: (Int) -> Unit = {}
) {
    val defaultItems = remember(activePreset, pinnedSounds.value) {
        soundItems.filter { pinnedSounds.value.contains(it.sound) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 预设切换行（滚动式 chip）
                if (presetList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presetList) { entry ->
                                FilterChip(
                                    onClick = { if (isEditMode) onRenamePreset(entry.id) else onActivePresetChange(entry.id) },
                                    label = { Text(entry.name) },
                                    selected = activePreset == entry.id,
                                    trailingIcon = if (isEditMode && presetList.size > 1) {
                                        {
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 4.dp)
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                                    .clickable { onDeletePreset(entry.id) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = context.getString(R.string.remove),
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                        }
                        if (!isEditMode && presetList.size < Constants.PrefsKeys.MAX_PRESET_COUNT) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .widthIn(min = 32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onAddPreset() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(0.dp))
                        }
                        // 编辑按钮
                        if (showEditButton) {
                            if (isEditMode) {
                                Surface(
                                    onClick = { onEditModeChange(false) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxHeight(),
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
                                IconButton(
                                    onClick = { onEditModeChange(true) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = context.getString(R.string.edit),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (isEditMode) {
                        Text(
                            text = context.getString(R.string.preset_rename_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    }
                }

                // 卡片区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val pinnedRemoteSounds = remoteSounds.filter { remotePinned.contains(it.id) }
                    val allDefaultItems = defaultItems.size + pinnedRemoteSounds.size

                    if (allDefaultItems == 0) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(104.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = context.getString(R.string.preset_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (verticalLayout) {
                        val presetCacheManager = remember { org.xmsleep.app.audio.AudioCacheManager.getInstance(context) }
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(defaultItems) { item: SoundItem ->
                                PresetCard(
                                    name = item.name,
                                    isPlaying = (playingStates[item.sound] == true) && (soundPlayingPreset[item.sound] == activePreset),
                                    isEditMode = isEditMode,
                                    onToggle = {
                                        val wasPlaying = audioManager.isPlayingSound(item.sound)
                                        if (wasPlaying) {
                                            audioManager.pauseSound(item.sound)
                                            playingStates[item.sound] = false
                                            soundPlayingPreset.remove(item.sound)
                                        } else {
                                            playingStates[item.sound] = true
                                            soundPlayingPreset[item.sound] = activePreset
                                            audioManager.playSound(context, item.sound)
                                            scope.launch {
                                                delay(200)
                                                playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                                            }
                                        }
                                    },
                                    onRemove = {
                                        val currentSet = pinnedSounds.value.toMutableSet()
                                        currentSet.remove(item.sound)
                                        pinnedSounds.value = currentSet
                                        if (audioManager.isPlayingSound(item.sound)) {
                                            audioManager.pauseSound(item.sound)
                                            playingStates[item.sound] = false
                                        }
                                    },
                                    isLocal = true
                                )
                            }
                            items(pinnedRemoteSounds) { sound: org.xmsleep.app.audio.model.SoundMetadata ->
                                val downloadProgress = downloadingSounds[sound.id]
                                val isPlaying = playingRemoteSounds.contains(sound.id)
                                PresetCard(
                                    name = getSoundDisplayName(sound),
                                    isPlaying = isPlaying,
                                    isEditMode = isEditMode,
                                    isCached = presetCacheManager.getCachedFile(sound.id) != null,
                                    isDownloading = downloadProgress != null,
                                    downloadProgress = downloadProgress,
                                    onToggle = { onRemoteCardClick(sound) },
                                    onRemove = {
                                        onRemotePinnedChange(sound.id, false)
                                        audioManager.pauseRemoteSound(sound.id)
                                    },
                                    isLocal = false
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(defaultItems) { item ->
                                DefaultCard(
                                    modifier = Modifier.width(100.dp),
                                    item = item,
                                    isPlaying = playingStates[item.sound] ?: false,
                                    showPlayingIndicator = (playingStates[item.sound] == true) && (soundPlayingPreset[item.sound] == activePreset),
                                    isEditMode = isEditMode,
                                    onToggle = { sound ->
                                        Logger.d("SoundsScreen", "DefaultCard onToggle: ${sound.name}")
                                        val wasPlaying = audioManager.isPlayingSound(sound)
                                        if (wasPlaying) {
                                            audioManager.pauseSound(sound)
                                            playingStates[sound] = false
                                            soundPlayingPreset.remove(sound)
                                        } else {
                                            playingStates[sound] = true
                                            soundPlayingPreset[sound] = activePreset
                                            audioManager.playSound(context, sound)
                                            scope.launch {
                                                delay(200)
                                                playingStates[sound] = audioManager.isPlayingSound(sound)
                                            }
                                        }
                                    },
                                    onRemove = {
                                        val currentSet = pinnedSounds.value.toMutableSet()
                                        currentSet.remove(item.sound)
                                        pinnedSounds.value = currentSet
                                        if (audioManager.isPlayingSound(item.sound)) {
                                            audioManager.pauseSound(item.sound)
                                            playingStates[item.sound] = false
                                        }
                                    },
                                    onPinnedChange = { isPinned ->
                                        val currentSet = pinnedSounds.value.toMutableSet()
                                        if (isPinned) {
                                            currentSet.add(item.sound)
                                            playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                                        } else {
                                            currentSet.remove(item.sound)
                                        }
                                        pinnedSounds.value = currentSet
                                        onPinnedChange(item.sound, isPinned)
                                    }
                                )
                            }

                            items(pinnedRemoteSounds) { sound ->
                                val downloadProgress = downloadingSounds[sound.id]
                                val isPlaying = playingRemoteSounds.contains(sound.id)
                                Box(modifier = Modifier.width(100.dp)) {
                                    org.xmsleep.app.ui.starsky.RemoteSoundCard(
                                        sound = sound,
                                        displayName = getSoundDisplayName(sound),
                                        isPlaying = isPlaying,
                                        downloadProgress = downloadProgress,
                                        columnsCount = 3,
                                        isPinned = remotePinned.contains(sound.id),
                                        onPinnedChange = { isPinned -> onRemotePinnedChange(sound.id, isPinned) },
                                        onCardClick = { onRemoteCardClick(sound) },
                                        onVolumeClick = { },
                                        cardHeight = 80.dp,
                                        isEditMode = isEditMode,
                                        onRemove = {
                                            onRemotePinnedChange(sound.id, false)
                                            audioManager.pauseRemoteSound(sound.id)
                                        },
                                        isInPresetDialog = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PresetCard(
    name: String,
    isPlaying: Boolean,
    isEditMode: Boolean,
    isLocal: Boolean,
    isCached: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = !isEditMode) { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isPlaying) 1f else 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPlaying) {
                    AudioVisualizer(
                        isPlaying = true,
                        modifier = Modifier.size(24.dp, 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!isLocal) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (isCached) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isEditMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = context.getString(R.string.remove),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 占位卡片（➕号）
 */
@Composable
internal fun PlaceholderCard(
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
            modifier = Modifier.fillMaxSize(),
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
 * 默认卡片（简化版，横向滚动用）
 */
@Composable
internal fun DefaultCard(
    modifier: Modifier = Modifier,
    item: SoundItem,
    isPlaying: Boolean,
    showPlayingIndicator: Boolean = isPlaying,
    isEditMode: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onRemove: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(enabled = !isEditMode) { onToggle(item.sound) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            if (isEditMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(y = 8.dp).size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = context.getString(R.string.remove),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).alpha(alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPlayingIndicator) {
                AudioVisualizer(
                    isPlaying = true,
                    modifier = Modifier.align(Alignment.BottomStart).size(24.dp, 16.dp).alpha(alpha),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * 内置声音内容（全部内置声音列表）
 */
@Composable
internal fun BuiltInSoundsContent(
    soundItems: List<SoundItem>,
    playingStates: MutableMap<AudioManager.Sound, Boolean>,
    audioManager: AudioManager,
    context: android.content.Context,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    scrollState: LazyGridState,
    onEditModeReset: () -> Unit,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    // 水平滑动布局（columnsCount == 1）
    if (columnsCount == 1 && soundItems.isNotEmpty()) {
        // 循环滑动：通过重复列表实现无限滚动效果
        val infiniteItemCount = if (soundItems.size > 1) soundItems.size * 100 else soundItems.size
        val initialPage = if (soundItems.size > 1) soundItems.size * 50 else 0
        
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { infiniteItemCount }
        )
        
        Column(modifier = Modifier.fillMaxSize()) {
            // 水平滑动卡片区域（垂直居中）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .padding(top = 24.dp),
                    contentPadding = PaddingValues(horizontal = 56.dp),
                    pageSpacing = 16.dp
                ) { pageIndex ->
                    // 循环滑动：取模获取实际索引
                    val actualIndex = if (soundItems.size > 1) pageIndex % soundItems.size else pageIndex
                    val item = soundItems[actualIndex]
                    var showVolumeDialog by remember { mutableStateOf(false) }
                    
                    // 根据与当前页的距离计算透明度和缩放
                    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    val absOffset = kotlin.math.abs(pageOffset)
                    val alpha = (1f - absOffset * 0.4f).coerceIn(0.5f, 1f)
                    val scale = (1f - absOffset * 0.08f).coerceIn(0.88f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight() // 填满pager高度
                                .graphicsLayer {
                                    this.alpha = alpha
                                    this.scaleX = scale
                                    this.scaleY = scale
                                }
                        ) {
                            // 使用专门的水平滑动卡片组件
                            HorizontalSoundCard(
                                item = item,
                                isPlaying = playingStates[item.sound] ?: false,
                                hideAnimation = hideAnimation,
                                isPinned = pinnedSounds.value.contains(item.sound),
                                onToggle = { sound ->
                                onEditModeReset()
                                val wasPlaying = audioManager.isPlayingSound(sound)
                                if (wasPlaying) {
                                    audioManager.pauseSound(sound)
                                    playingStates[sound] = false
                                } else {
                                    playingStates[sound] = true
                                    audioManager.playSound(context, sound)
                                    scope.launch {
                                        delay(200)
                                        playingStates[sound] = audioManager.isPlayingSound(sound)
                                    }
                                }
                            },
                            onVolumeClick = { showVolumeDialog = true },
                            onTitleClick = { },
                            onPinnedChange = { isPinned ->
                                val currentSet = pinnedSounds.value.toMutableSet()
                                if (isPinned) {
                                    currentSet.add(item.sound)
                                    playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                                } else {
                                    currentSet.remove(item.sound)
                                }
                                pinnedSounds.value = currentSet
                                onPinnedChange(item.sound, isPinned)
                            }
                        )
                        if (showVolumeDialog) {
                            VolumeDialog(
                                sound = item.sound,
                                currentVolume = audioManager.getVolume(item.sound),
                                onDismiss = { showVolumeDialog = false },
                                onVolumeChange = { audioManager.setVolume(item.sound, it) }
                            )
                        }
                    }
                }
            }
        }
        }
    } else {
        // 网格布局（columnsCount == 2 或 3）
        Column(modifier = Modifier.fillMaxSize()) {
            if (soundItems.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = scrollState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(soundItems) { item ->
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        SoundCard(
                            item = item,
                            isPlaying = playingStates[item.sound] ?: false,
                            hideAnimation = hideAnimation,
                            columnsCount = columnsCount,
                            isPinned = pinnedSounds.value.contains(item.sound),
                            onToggle = { sound ->
                                onEditModeReset()
                                val wasPlaying = audioManager.isPlayingSound(sound)
                                if (wasPlaying) {
                                    audioManager.pauseSound(sound)
                                    playingStates[sound] = false
                                } else {
                                    playingStates[sound] = true
                                    audioManager.playSound(context, sound)
                                    scope.launch {
                                        delay(200)
                                        playingStates[sound] = audioManager.isPlayingSound(sound)
                                    }
                                }
                            },
                            onVolumeClick = { showVolumeDialog = true },
                            onTitleClick = { },
                            onPinnedChange = { isPinned ->
                                val currentSet = pinnedSounds.value.toMutableSet()
                                if (isPinned) {
                                    currentSet.add(item.sound)
                                    playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                                } else {
                                    currentSet.remove(item.sound)
                                }
                                pinnedSounds.value = currentSet
                                onPinnedChange(item.sound, isPinned)
                            }
                        )
                        if (showVolumeDialog) {
                            VolumeDialog(
                                sound = item.sound,
                                currentVolume = audioManager.getVolume(item.sound),
                                onDismiss = { showVolumeDialog = false },
                                onVolumeChange = { audioManager.setVolume(item.sound, it) }
                            )
                        }
                    }
                    // XMSLEEP 品牌文字
                    item(span = { GridItemSpan(columnsCount) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "XMSLEEP",
                                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary.copy(alpha = 0.6f),
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = stringResource(R.string.wish_good_sleep),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 线上声音内容（待上线，当前显示空状态）
 */
@Composable
internal fun OnlineSoundsContent() {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EmptyStateAnimation(animationSize = 200.dp)
            Text(context.getString(R.string.no_online_content), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(context.getString(R.string.online_sounds_will_show_here), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
