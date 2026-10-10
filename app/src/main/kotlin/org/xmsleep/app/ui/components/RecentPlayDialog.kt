package org.xmsleep.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager

/**
 * 最近播放弹窗
 * 在应用启动时显示，提示用户可以快速播放上次的音频
 * 注意：只显示提示，不自动播放
 */
@Composable
fun RecentPlayDialog(
    onDismiss: () -> Unit,
    onPlayRecent: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { AudioManager.getInstance() }
    
    // 检查是否有最近播放的声音
    val hasRecentSounds = remember { audioManager.hasRecentSounds(context) }
    
    // 如果没有最近播放的声音，不显示弹窗
    if (!hasRecentSounds) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }
    
    // 自动消失的倒计时
    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(1500) // 1.5秒后自动消失
        visible = false
        delay(500) // 等待动画完成
        onDismiss()
    }
    
    // 毛玻璃浮层容器：背景磨砂模糊，点背景关闭、点卡片播放
    FrostedGlassSheet(
        visible = visible,
        onDismissRequest = { visible = false }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        // 点击卡片播放最近的音频
                        onPlayRecent()
                        visible = false
                    },
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 播放按钮图标
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // 文字
            Column {
                Text(
                    text = stringResource(R.string.recent_play),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.tap_to_play),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
