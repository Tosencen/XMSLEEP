package org.xmsleep.app.ui.flipclock

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.xmsleep.app.R
import org.xmsleep.app.ui.components.WheelPicker
import kotlin.math.min

/**
 * 翻页时钟主页面
 * 背景透明以显示动画背景，使用主题颜色
 */
@Composable
fun FlipClockScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlipClockViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showFontSelector by remember { mutableStateOf(false) }

    // 保持屏幕常亮
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val (displayHours, displayMinutes, displaySeconds) = if (uiState.isCountdownMode && uiState.isCountdownActive) {
        val (h, m, s) = viewModel.getCountdownTime()
        Triple(h, m, s)
    } else if (uiState.isCountdownMode && !uiState.isCountdownActive) {
        Triple(0, 0, 0)
    } else {
        Triple(uiState.hours, uiState.minutes, uiState.seconds)
    }

    // 背景透明，让后面的动画背景显示出来
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
    ) {
        // 翻页时钟主体
        FlipClockContent(
            hours = displayHours,
            minutes = displayMinutes,
            seconds = displaySeconds,
            isLandscape = isLandscape,
            clockFont = uiState.selectedFont,
            modifier = Modifier.align(Alignment.Center)
        )

        // 倒计时显示（已移除文案，只通过时钟数字显示）

        // 功能栏
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FlipClockControls(
                context = context,
                onBack = onBack,
                onCountdownClick = { showCountdownDialog = true },
                onFontClick = { showFontSelector = true },
                isCountdownActive = uiState.isCountdownActive,
                onCancelCountdown = { viewModel.cancelCountdown() }
            )
        }

        // 倒计时设置弹窗
        if (showCountdownDialog) {
            CountdownDialog(
                context = context,
                onDismiss = { showCountdownDialog = false },
                onConfirm = { minutes ->
                    viewModel.startCountdown(minutes)
                    showCountdownDialog = false
                }
            )
        }

        // 字体选择弹窗
        if (showFontSelector) {
            FontSelectorDialog(
                context = context,
                currentFont = uiState.selectedFont,
                onDismiss = { showFontSelector = false },
                onSelect = { font ->
                    viewModel.setFont(font)
                    showFontSelector = false
                }
            )
        }
    }
}

@Composable
private fun FlipClockContent(
    hours: Int,
    minutes: Int,
    seconds: Int,
    isLandscape: Boolean,
    clockFont: ClockFont,
    modifier: Modifier = Modifier
) {
    val fontFamily = clockFont.fontFamily
    val verticalOffset = clockFont.verticalOffset
    
    // 使用 BoxWithConstraints 实现自适应布局
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        
        // 直接根据 BoxWithConstraints 的尺寸判断方向，确保一致性
        val isLandscapeLayout = maxWidth > maxHeight
        
        // 根据屏幕方向计算卡片尺寸（1:1正方形，4个等间距）
        // 布局：间距 - 卡片 - 间距 - 卡片 - 间距 = 4个间距 + 2个卡片
        // 间距 : 卡片 = 0.5 : 4.0
        val (cardSize, spacing) = if (isLandscapeLayout) {
            // 横屏：基于宽度计算，4个间距 + 2个卡片
            // 4*0.5 + 2*4.0 = 2 + 8 = 10份
            val availableWidth = maxWidth
            val unit = availableWidth / 10f // 每份大小
            val cardS = minOf(unit * 4.0f, maxHeight * 0.9f) // 卡片 = 4.0份
            val space = unit * 0.5f // 间距 = 0.5份
            Pair(cardS, space)
        } else {
            // 竖屏：基于高度计算，4个间距 + 2个卡片
            val availableHeight = maxHeight
            val unit = availableHeight / 10f
            val cardS = minOf(unit * 4.0f, maxWidth * 0.95f) // 卡片 = 4.0份
            val space = unit * 0.5f // 间距 = 0.5份
            Pair(cardS, space)
        }
        
        // 根据卡片高度自适应字体大小
        val adaptiveFontSize = (cardSize.value * 0.7f).toInt().coerceIn(120, 280)
        
        if (isLandscapeLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlipCard(
                    value = hours % 24,
                    fontFamily = fontFamily,
                    verticalOffset = verticalOffset,
                    fontSize = adaptiveFontSize,
                    modifier = Modifier.size(cardSize)
                )
                FlipCardWithSeconds(
                    value = minutes,
                    seconds = seconds,
                    fontFamily = fontFamily,
                    verticalOffset = verticalOffset,
                    fontSize = adaptiveFontSize,
                    modifier = Modifier.size(cardSize)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterVertically)
            ) {
                FlipCard(
                    value = hours % 24,
                    fontFamily = fontFamily,
                    verticalOffset = verticalOffset,
                    fontSize = adaptiveFontSize,
                    modifier = Modifier.size(cardSize)
                )
                FlipCardWithSeconds(
                    value = minutes,
                    seconds = seconds,
                    fontFamily = fontFamily,
                    verticalOffset = verticalOffset,
                    fontSize = adaptiveFontSize,
                    modifier = Modifier.size(cardSize)
                )
            }
        }
    }
}

@Composable
private fun FlipClockControls(
    context: android.content.Context,
    onBack: () -> Unit,
    onCountdownClick: () -> Unit,
    onFontClick: () -> Unit,
    isCountdownActive: Boolean,
    onCancelCountdown: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = context.getString(R.string.flip_clock_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isCountdownActive) {
                FilledTonalIconButton(
                    onClick = onCancelCountdown,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = context.getString(R.string.flip_clock_cancel_countdown),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                FilledTonalIconButton(
                    onClick = onCountdownClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = context.getString(R.string.flip_clock_countdown),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            FilledTonalIconButton(
                onClick = onFontClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = context.getString(R.string.flip_clock_font),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CountdownDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(0) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    val quickOptions = listOf(15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = context.getString(R.string.flip_clock_set_countdown),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickOptions.take(3).forEach { minutes ->
                            FilterChip(
                                selected = (selectedHour * 60 + selectedMinute) == minutes,
                                onClick = {
                                    selectedHour = minutes / 60
                                    selectedMinute = minutes % 60
                                },
                                label = { Text(context.getString(R.string.flip_clock_minutes_format, minutes)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickOptions.drop(3).forEach { minutes ->
                            FilterChip(
                                selected = (selectedHour * 60 + selectedMinute) == minutes,
                                onClick = {
                                    selectedHour = minutes / 60
                                    selectedMinute = minutes % 60
                                },
                                label = { Text(context.getString(R.string.flip_clock_minutes_format, minutes)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // 自定义时间轮盘选择器
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = context.getString(R.string.hours),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val hourItems = remember { (0..24).map { it.toString().padStart(2, '0') } }
                            WheelPicker(
                                options = hourItems,
                                selectedIndex = selectedHour,
                                onItemSelected = { selectedHour = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = context.getString(R.string.minutes_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val minuteItems = remember { (0..59).map { it.toString().padStart(2, '0') } }
                            WheelPicker(
                                options = minuteItems,
                                selectedIndex = selectedMinute,
                                onItemSelected = { selectedMinute = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // 显示当前选择的总时间
                val totalMinutes = selectedHour * 60 + selectedMinute
                Text(
                    text = if (totalMinutes > 0) {
                        if (selectedHour > 0) {
                            context.getString(R.string.hours_minutes, selectedHour, selectedMinute)
                        } else {
                            context.getString(R.string.minutes_only, selectedMinute)
                        }
                    } else {
                        context.getString(R.string.minutes_only, 0)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMinutes = selectedHour * 60 + selectedMinute
                    if (totalMinutes > 0) {
                        onConfirm(totalMinutes)
                    }
                }
            ) {
                Text(context.getString(R.string.flip_clock_start), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.flip_clock_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun FontSelectorDialog(
    context: android.content.Context,
    currentFont: ClockFont,
    onDismiss: () -> Unit,
    onSelect: (ClockFont) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = context.getString(R.string.flip_clock_select_font),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClockFont.entries.forEach { font ->
                    val isSelected = font == currentFont
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(font) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = font.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                )
                                Text(
                                    text = context.getString(font.descriptionResId),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        }
                                    )
                                )
                            }
                            // 预览数字
                            Text(
                                text = "88",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = font.fontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.flip_clock_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}
