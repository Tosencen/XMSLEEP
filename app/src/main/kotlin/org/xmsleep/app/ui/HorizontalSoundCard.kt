package org.xmsleep.app.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager

/**
 * 水平滑动布局的声音卡片（支持自定义高度）
 */
@Composable
fun HorizontalSoundCard(
    item: SoundItem,
    isPlaying: Boolean,
    hideAnimation: Boolean = false,
    isPinned: Boolean = false,
    cardHeight: Dp = 220.dp,
    onToggle: (AudioManager.Sound) -> Unit,
    onVolumeClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onPinnedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTitleMenu by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.6f,
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight() // 填满父容器高度
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggle(item.sound) },
                    onLongPress = { showTitleMenu = true }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // WebP 动画内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // WebP 动画（居中显示）
                AnimatedWebPImage(
                    drawableResId = item.animationRes,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp) // 动画固定大小
                        .alpha(alpha),
                    contentScale = ContentScale.Fit,
                    isPlaying = isPlaying
                )
                
                // 标题（底部）
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .alpha(alpha)
                )
                
                // 音量按钮（右下角）
                if (isPlaying) {
                    IconButton(
                        onClick = onVolumeClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
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
                
                // 固定图标（右上角）
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // 长按菜单（只有固定/取消固定功能）
                DropdownMenu(
                    expanded = showTitleMenu,
                    onDismissRequest = { showTitleMenu = false },
                    modifier = Modifier.width(160.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    // 固定/取消固定
                    DropdownMenuItem(
                        text = { Text(if (isPinned) context.getString(R.string.cancel_default) else context.getString(R.string.set_as_default)) },
                        onClick = {
                            val newState = !isPinned
                            onPinnedChange(newState)
                            showTitleMenu = false
                            org.xmsleep.app.utils.ToastUtils.showToast(
                                context,
                                if (newState) context.getString(R.string.pinned_success) else context.getString(R.string.unpinned_success)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}
