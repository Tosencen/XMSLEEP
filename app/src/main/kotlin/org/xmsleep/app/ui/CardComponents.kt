package org.xmsleep.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager

/**
 * 无动画卡片组件 - 2列布局（100.dp高度）
 * 用于首页2列布局，不显示Lottie动画
 */
@Composable
fun SimpleSoundCard2Columns(
    item: SoundItem,
    isPlaying: Boolean,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTitleMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggle(item.sound) },
                    onLongPress = {
                        scope.launch {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 标题（左上角）
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 菜单弹窗
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                DropdownMenu(
                    expanded = showTitleMenu,
                    onDismissRequest = { showTitleMenu = false },
                    modifier = Modifier.width(120.dp),
                    shape = RoundedCornerShape(12.dp)
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
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
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
            }
            
            // 音频可视化器（左下角，只在播放时显示）
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
                        .offset(x = 10.dp, y = 12.dp)
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

/**
 * 无动画卡片组件 - 3列布局（80.dp高度）
 * 用于首页3列布局，不显示Lottie动画
 */
@Composable
fun SimpleSoundCard3Columns(
    item: SoundItem,
    isPlaying: Boolean,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTitleMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggle(item.sound) },
                    onLongPress = {
                        scope.launch {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 标题（左上角）
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 菜单弹窗
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                DropdownMenu(
                    expanded = showTitleMenu,
                    onDismissRequest = { showTitleMenu = false },
                    modifier = Modifier.width(120.dp),
                    shape = RoundedCornerShape(12.dp)
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
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
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
            }
            
            // 音频可视化器（左下角，只在播放时显示）
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
                        .offset(x = 10.dp, y = 12.dp)
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

/**
 * 快捷播放卡片组件（80.dp高度）
 * 用于快捷播放模块，不显示Lottie动画，不显示音量按钮
 */
@Composable
fun QuickPlayCard(
    item: SoundItem,
    isPlaying: Boolean,
    isEditMode: Boolean = false,
    onToggle: (AudioManager.Sound) -> Unit,
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier
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
            // 删除按钮（右下角，只在编辑模式下显示）
            if (isEditMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 8.dp)
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
            
            // 音频可视化器（左下角，只在播放时显示）
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

