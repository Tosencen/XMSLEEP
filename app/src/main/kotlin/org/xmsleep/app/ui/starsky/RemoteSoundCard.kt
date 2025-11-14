package org.xmsleep.app.ui.starsky

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.ui.AudioVisualizer

/**
 * 远程音频卡片组件
 * 用于星空页面和快捷播放模块
 */
@Composable
fun RemoteSoundCard(
    sound: org.xmsleep.app.audio.model.SoundMetadata,
    displayName: String,
    isPlaying: Boolean,
    downloadProgress: Float?,
    isDownloadingButNoProgress: Boolean = false,
    columnsCount: Int = 2,
    isPinned: Boolean = false,
    isFavorite: Boolean = false,
    onPinnedChange: (Boolean) -> Unit = {},
    onFavoriteChange: (Boolean) -> Unit = {},
    onCardClick: () -> Unit,
    onVolumeClick: () -> Unit,
    cardHeight: Dp? = null,
    isEditMode: Boolean = false,
    onRemove: () -> Unit = {}
) {
    val context = LocalContext.current
    val cacheManager = remember { 
        org.xmsleep.app.audio.AudioCacheManager.getInstance(context) 
    }
    var isCached by remember(sound.id) { 
        mutableStateOf(cacheManager.getCachedFile(sound.id) != null) 
    }
    
    // 监听下载完成，更新缓存状态
    LaunchedEffect(downloadProgress, sound.id, isPlaying) {
        val cached = cacheManager.getCachedFile(sound.id) != null
        if (cached != isCached) {
            isCached = cached
        }
        
        if (downloadProgress != null && downloadProgress >= 1.0f) {
            delay(300)
            val newCached = cacheManager.getCachedFile(sound.id) != null
            if (newCached != isCached) {
                isCached = newCached
            }
        }
        
        if (downloadProgress == null) {
            delay(300)
            val newCached = cacheManager.getCachedFile(sound.id) != null
            if (newCached != isCached) {
                isCached = newCached
            }
        }
        
        if (isPlaying) {
            val playingCached = cacheManager.getCachedFile(sound.id) != null
            if (playingCached != isCached) {
                isCached = playingCached
            }
        }
    }
    
    LaunchedEffect(Unit) {
        delay(100)
        val cached = cacheManager.getCachedFile(sound.id) != null
        if (cached != isCached) {
            isCached = cached
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )
    
    val cardBackgroundColor = if (isCached) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    
    var showTitleMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val finalCardHeight = cardHeight ?: if (columnsCount == 3) 80.dp else 100.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(finalCardHeight)
            .then(
                if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onCardClick() },
                            onLongPress = { 
                                scope.launch {
                                    showTitleMenu = true
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        border = if (!isCached) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        } else {
            null
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val padding = if (finalCardHeight == 80.dp) 12.dp else 16.dp
            val textStyle = if (finalCardHeight == 80.dp) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.titleMedium
            }
            val maxLines = if (finalCardHeight == 80.dp) 1 else 2
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 标题
                Box(modifier = Modifier.align(Alignment.TopStart)) {
                    Text(
                        text = displayName,
                        style = textStyle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(alpha),
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 加载指示器
                if (isDownloadingButNoProgress && (downloadProgress == null || downloadProgress == 0f)) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 菜单
                if (!isEditMode) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
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
                                            tint = if (isPinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = if (isPinned) {
                                                context.getString(R.string.cancel_default)
                                            } else {
                                                context.getString(R.string.set_as_default)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isPinned) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    onPinnedChange(!isPinned)
                                    showTitleMenu = false
                                }
                            )
                            
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) {
                                                Icons.Default.Star
                                            } else {
                                                Icons.Default.StarOutline
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (isFavorite) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = if (isFavorite) {
                                                context.getString(R.string.cancel_favorite)
                                            } else {
                                                context.getString(R.string.favorite)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isFavorite) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    onFavoriteChange(!isFavorite)
                                    showTitleMenu = false
                                }
                            )
                            
                            if (isCached) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = context.getString(R.string.delete_cache),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            try {
                                                cacheManager.deleteCache(sound.id)
                                                val cachedFile = cacheManager.getCachedFile(sound.id)
                                                if (cachedFile == null) {
                                                    isCached = false
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.cache_cleared_success),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.cache_clear_failed, e.message ?: ""),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            showTitleMenu = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // 音频可视化器
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
                
                // 删除按钮（编辑模式）
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
                
                // 音量按钮
                if (isPlaying && cardHeight == null && !isEditMode) {
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
            
            // 下载进度条
            if (downloadProgress != null && downloadProgress > 0f) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}
