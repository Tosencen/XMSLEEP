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
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.utils.ToastUtils
import java.util.concurrent.TimeUnit

/**
 * 声音卡片
 */
@Composable
fun SoundCard(
    item: SoundItem,
    isPlaying: Boolean,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    isPinned: Boolean = false,
    isBatchSelectMode: Boolean = false,
    isSelected: Boolean = false,
    canSelect: Boolean = true,
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
    val cardAlpha by animateFloatAsState(
        targetValue = if (isBatchSelectMode && !canSelect && !isSelected) 0.5f else 1f,
        label = "cardAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (hideAnimation) 100.dp else 140.dp)
            .alpha(cardAlpha)
            .pointerInput(isBatchSelectMode, isSelected, canSelect) {
                detectTapGestures(
                    onTap = {
                        if (isBatchSelectMode) {
                            if (isSelected || canSelect) onToggle(item.sound)
                        } else {
                            onToggle(item.sound)
                        }
                    },
                    onLongPress = {
                        if (!isBatchSelectMode) showTitleMenu = true
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 批量选择模式选择框
            if (isBatchSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isSelected) context.getString(R.string.selected) else context.getString(R.string.unselected),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hideAnimation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (columnsCount == 3) {
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.alpha(alpha)
                            )
                        }
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SoundCardMenuItems(
                                isPinned = isPinned,
                                onPinnedChange = onPinnedChange,
                                onDismiss = { showTitleMenu = false }
                            )
                        }
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
                    } else {
                        Box(modifier = Modifier.align(Alignment.TopStart)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.alpha(alpha)
                            )
                        }
                        DropdownMenu(
                            expanded = showTitleMenu,
                            onDismissRequest = { showTitleMenu = false },
                            modifier = Modifier.width(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SoundCardMenuItems(
                                isPinned = isPinned,
                                onPinnedChange = onPinnedChange,
                                onDismiss = { showTitleMenu = false }
                            )
                        }
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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alpha(alpha)
                        )
                    }
                    DropdownMenu(
                        expanded = showTitleMenu,
                        onDismissRequest = { showTitleMenu = false },
                        modifier = Modifier.width(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        SoundCardMenuItems(
                            isPinned = isPinned,
                            onPinnedChange = onPinnedChange,
                            onDismiss = { showTitleMenu = false }
                        )
                    }
                    AnimatedWebPImage(
                        drawableResId = item.animationRes,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(80.dp)
                            .alpha(alpha),
                        contentScale = ContentScale.Fit,
                        isPlaying = isPlaying
                    )
                }
                if (isPlaying) {
                    IconButton(
                        onClick = onVolumeClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = (-4).dp)
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
}

/**
 * SoundCard 菜单项（仅置顶），抽取复用
 */
@Composable
private fun SoundCardMenuItems(
    isPinned: Boolean,
    onPinnedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
            val newState = !isPinned
            onPinnedChange(newState)
            onDismiss()
            ToastUtils.showToast(
                context,
                if (newState) context.getString(R.string.pinned_success) else context.getString(R.string.unpinned_success)
            )
        }
    )
}

/**
 * 动画 WebP 图片组件（支持动画 WebP 播放、主题色适配）
 */
@Composable
internal fun AnimatedWebPImage(
    drawableResId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    isPlaying: Boolean = false
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val themeColor = colorScheme.primary
    val themeHct = remember(themeColor) { themeColor.toHct() }
    val darkColor = remember(themeHct) {
        androidx.compose.ui.graphics.Color(Hct.from(themeHct.hue, themeHct.chroma, minOf(themeHct.tone - 20, 40.0)).toInt())
    }
    val lightColor = remember(themeHct) {
        androidx.compose.ui.graphics.Color(Hct.from(themeHct.hue, themeHct.chroma, maxOf(themeHct.tone + 20, 60.0)).toInt())
    }
    var animatedDrawable by remember { mutableStateOf<AnimatedImageDrawable?>(null) }
    LaunchedEffect(isPlaying, animatedDrawable) {
        val drawable = animatedDrawable ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (isPlaying) {
                if (!drawable.isRunning) drawable.start()
            } else {
                if (drawable.isRunning) drawable.stop()
            }
        }
    }
    val colorMatrix = remember(darkColor, lightColor) {
        val darkArgb = darkColor.toArgb()
        val lightArgb = lightColor.toArgb()
        val darkR = ((darkArgb shr 16) and 0xFF) / 255f
        val darkG = ((darkArgb shr 8) and 0xFF) / 255f
        val darkB = (darkArgb and 0xFF) / 255f
        val lightR = ((lightArgb shr 16) and 0xFF) / 255f
        val lightG = ((lightArgb shr 8) and 0xFF) / 255f
        val lightB = (lightArgb and 0xFF) / 255f
        val deltaR = lightR - darkR
        val deltaG = lightG - darkG
        val deltaB = lightB - darkB
        android.graphics.ColorMatrix().apply {
            val matrix = FloatArray(20)
            matrix[0] = deltaR * 0.299f; matrix[1] = deltaR * 0.587f; matrix[2] = deltaR * 0.114f; matrix[3] = 0f; matrix[4] = darkR * 255f
            matrix[5] = deltaG * 0.299f; matrix[6] = deltaG * 0.587f; matrix[7] = deltaG * 0.114f; matrix[8] = 0f; matrix[9] = darkG * 255f
            matrix[10] = deltaB * 0.299f; matrix[11] = deltaB * 0.587f; matrix[12] = deltaB * 0.114f; matrix[13] = 0f; matrix[14] = darkB * 255f
            matrix[15] = 0f; matrix[16] = 0f; matrix[17] = 0f; matrix[18] = 1f; matrix[19] = 0f
            set(matrix)
        }
    }
    AndroidView(
        factory = { ctx ->
            val iv = android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(ctx.resources, drawableResId)
                val decoded = ImageDecoder.decodeDrawable(source)
                val anim = decoded as? AnimatedImageDrawable
                animatedDrawable = anim
                if (anim != null) {
                    anim.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                } else {
                    decoded.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                }
                iv.setImageDrawable(decoded)
            } else {
                iv.setImageResource(drawableResId)
                iv.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            }
            iv
        },
        update = { view ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                animatedDrawable?.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            } else {
                view.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            }
        },
        modifier = modifier
    )
}

/**
 * 倒计时 FAB 组件
 */
@Composable
fun TimerFAB(
    isTimerActive: Boolean,
    timeLeftMillis: Long,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    key(timeLeftMillis, isTimerActive, enabled) {
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = if (enabled) onClick else {
                    {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.please_play_sound_before_timer),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                containerColor = when {
                    !enabled -> MaterialTheme.colorScheme.secondaryContainer
                    isTimerActive -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = when {
                    !enabled -> MaterialTheme.colorScheme.onSecondaryContainer
                    isTimerActive -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onPrimary
                }
            ) {
                Icon(Icons.Default.Timer, contentDescription = context.getString(R.string.set_countdown))
            }
            if (isTimerActive && timeLeftMillis > 0) {
                val totalHours = TimeUnit.MILLISECONDS.toHours(timeLeftMillis)
                val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(timeLeftMillis)
                val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeLeftMillis)
                val hours = totalHours
                val minutes = totalMinutes % 60
                val seconds = totalSeconds % 60
                val timerText = if (hours > 0) {
                    "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
                } else {
                    "${minutes}:${seconds.toString().padStart(2, '0')}"
                }
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Text(text = timerText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * 预设播放 FAB 组件
 */
@Composable
internal fun PresetPlayFAB(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
        contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
        icon = {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null
            )
        },
        text = { Text(text = context.getString(R.string.preset)) }
    )
}
