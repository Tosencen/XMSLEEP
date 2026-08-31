package org.xmsleep.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.ui.AudioVisualizer

/**
 * 音频卡片的共用零件。
 *
 * 本地音频卡片（ui/SoundCard.kt）与远程音频卡片（ui/starsky/RemoteSoundCard.kt）
 * 此前各自抄了一份几乎逐字相同的实现，这里统一收口。
 *
 * 抽取原则：**只统一 UI，不统一业务判断**。
 * 两边的显示条件与点击行为本就不同（例如远程卡片置顶前要先校验是否已下载），
 * 强行合并会丢掉这些差异，因此条件判断全部留在调用方。
 */

/**
 * 播放中的音频可视化动画。
 *
 * @param isPlaying 是否正在播放
 * @param alpha     卡片整体的透明度（未下载等状态会整体变淡，动画需跟随）
 * @param modifier  由调用方传入定位，通常是 `Modifier.align(Alignment.BottomStart)`
 */
@Composable
fun CardPlayingIndicator(
    isPlaying: Boolean,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (isPlaying) {
        AudioVisualizer(
            isPlaying = true,
            modifier = modifier
                .size(24.dp, 16.dp)
                .alpha(alpha),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 卡片右下角的音量按钮。
 *
 * 注意：**是否显示由调用方决定**。本地卡片是 `isPlaying`，
 * 远程卡片还额外要求 `cardHeight == null && !isEditMode`，两者并不相同。
 *
 * @param modifier 由调用方传入定位，通常是 `Modifier.align(Alignment.BottomEnd)`
 * @param offsetX / @param offsetY 微调偏移。卡片布局不同取值也不同
 *        （带封面的卡片是 10/12，纯色卡片是 0/-4），故开放为参数而非写死
 */
@Composable
fun CardVolumeButton(
    onVolumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    offsetX: Dp = 10.dp,
    offsetY: Dp = 12.dp
) {
    val context = LocalContext.current
    IconButton(
        onClick = onVolumeClick,
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
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

/**
 * 置顶菜单项的图标 + 文案部分。
 *
 * 只抽取 UI，onClick 仍由调用方提供，因为两边的点击行为不同：
 * - 本地音频：直接切换置顶
 * - 远程音频：先校验是否已下载，未下载时提示「请先下载」并拦截
 */
@Composable
fun PinMenuItemContent(isPinned: Boolean) {
    val context = LocalContext.current
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
}

/**
 * 置顶状态切换后的提示文案（两边共用）。
 */
fun pinToggleToastMessage(context: Context, newPinned: Boolean): String =
    if (newPinned) {
        context.getString(R.string.pinned_success)
    } else {
        context.getString(R.string.unpinned_success)
    }

/**
 * 完整的置顶菜单项（UI + 默认点击行为），供本地音频卡片这类
 * 无需前置校验的场景直接使用。
 */
@Composable
fun PinMenuItem(
    isPinned: Boolean,
    onPinnedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { PinMenuItemContent(isPinned = isPinned) },
        onClick = {
            val newState = !isPinned
            onPinnedChange(newState)
            onDismiss()
            onToast(pinToggleToastMessage(context, newState))
        }
    )
}
