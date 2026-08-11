package org.xmsleep.app.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R
import org.xmsleep.app.analytics.AnalyticsLogger
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.ui.tomato.TomatoRingtonePlayer
import org.xmsleep.app.utils.Logger
import kotlin.math.roundToInt

private const val NOTIFICATION_CHANNEL_ID = "tomato_timer_channel"
private const val NOTIFICATION_ID = 1001
private const val TAG = "TomatoTimerView"

object TomatoTimerState {
    var isRunning = mutableStateOf(false)
    var isBreak = mutableStateOf(false)
    var selectedFocusMinutes = mutableIntStateOf(25)
    var timeLeftMillis = mutableLongStateOf(25L * 60 * 1000L)
    var todayCompletedPomodoros = mutableIntStateOf(0)
    // 专注结束后等待用户点击"开始休息"（不再自动进入休息）
    var awaitingBreakStart = mutableStateOf(false)
    // 完成事件计数：每完成一次递增，供页面触发完成动画
    var completionTick = mutableIntStateOf(0)

    // 休息时长（分钟），可在休息准备态通过刻度尺调整
    var breakDurationMinutes = mutableIntStateOf(5)

    // 截止时间（基于 SystemClock.elapsedRealtime），0 = 未在计时
    private var deadline = 0L

    // 应用级协程作用域：倒计时不依赖页面存活，离开页面仍在后台走
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var countdownJob: Job? = null
    private var appContext: Context? = null

    /**
     * 开始/恢复倒计时（点播放、开始休息时调用）。
     * 基于截止时间计算剩余，离开页面后仍继续倒计时并按时完成。
     */
    fun start(context: Context) {
        // 保存本地化的 application context（LocalContext 已按应用语言包装），
        // 保证后台通知等取字符串时与应用语言一致，而非系统语言
        appContext = context
        countdownJob?.cancel()
        awaitingBreakStart.value = false
        isRunning.value = true
        createNotificationChannel(appContext!!)
        val remaining = timeLeftMillis.longValue.coerceAtLeast(0)
        deadline = SystemClock.elapsedRealtime() + remaining
        showTimerNotification(appContext!!, remaining, isBreak.value)
        countdownJob = scope.launch {
            while (isActive) {
                val rem = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                timeLeftMillis.longValue = rem
                if (rem <= 0) {
                    handleCompletion()
                    break
                }
                updateNotification(appContext!!, rem, isBreak.value)
                delay(1000)
            }
        }
    }

    /**
     * 暂停倒计时（保留剩余时间）
     */
    fun pause() {
        countdownJob?.cancel()
        deadline = 0L
        isRunning.value = false
        appContext?.let { cancelNotification(it) }
    }

    /**
     * 重置回专注准备态
     */
    fun reset() {
        countdownJob?.cancel()
        deadline = 0L
        isRunning.value = false
        isBreak.value = false
        awaitingBreakStart.value = false
        timeLeftMillis.longValue = selectedFocusMinutes.intValue * 60 * 1000L
        appContext?.let { cancelNotification(it) }
    }

    /**
     * 跳过休息，回到专注准备态
     */
    fun skipBreak() {
        countdownJob?.cancel()
        deadline = 0L
        isBreak.value = false
        awaitingBreakStart.value = false
        timeLeftMillis.longValue = selectedFocusMinutes.intValue * 60 * 1000L
        isRunning.value = false
        appContext?.let { cancelNotification(it) }
    }

    /**
     * 到点完成：响铃、震动、完成通知，并切换阶段状态
     */
    private fun handleCompletion() {
        val ctx = appContext ?: return
        AnalyticsLogger.logTomatoComplete(
            phase = if (isBreak.value) "break" else "focus",
            focusMinutes = selectedFocusMinutes.intValue
        )
        countdownJob?.cancel()
        deadline = 0L
        isRunning.value = false
        if (!isBreak.value) todayCompletedPomodoros.intValue++
        // 播放结束铃声（内部会停止白噪音等），并显示完成通知
        TomatoRingtonePlayer.play(ctx, PreferencesManager.getTomatoRingtone(ctx))
        showCompletionNotification(ctx, isBreak.value)
        // 震动开关
        if (PreferencesManager.getTomatoVibrate(ctx)) {
            vibrate(ctx)
        }
        if (isBreak.value) {
            // 休息结束：回到专注准备态，等待用户手动开始下一轮
            isBreak.value = false
            timeLeftMillis.longValue = selectedFocusMinutes.intValue * 60 * 1000L
            awaitingBreakStart.value = false
        } else {
            // 专注结束：进入休息准备态，不自动开始休息
            isBreak.value = true
            timeLeftMillis.longValue = TomatoTimerState.breakDurationMinutes.intValue * 60 * 1000L
            awaitingBreakStart.value = true
        }
        completionTick.intValue++
    }
}

@Composable
fun TomatoTimerView(
    modifier: Modifier = Modifier,
    focusDurationMinutes: Int = 25,
    onTimerComplete: () -> Unit = {},
    onPulseStart: (isBreak: Boolean) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val view = LocalView.current

    var isRunning by TomatoTimerState.isRunning
    var isBreak by TomatoTimerState.isBreak
    var selectedFocusMinutes by TomatoTimerState.selectedFocusMinutes
    var breakDurationMinutes by TomatoTimerState.breakDurationMinutes
    var timeLeftMillis by TomatoTimerState.timeLeftMillis
    var todayCompletedPomodoros by TomatoTimerState.todayCompletedPomodoros
    var awaitingBreakStart by TomatoTimerState.awaitingBreakStart
    var completionTick by TomatoTimerState.completionTick

    // 完成事件消费标记：仅在完成的那一次触发动画/回调（含离开页面后台完成的场景）
    var lastCompletionTick by rememberSaveable { mutableIntStateOf(TomatoTimerState.completionTick.intValue) }
    LaunchedEffect(completionTick) {
        if (completionTick != lastCompletionTick) {
            lastCompletionTick = completionTick
            if (PreferencesManager.getTomatoPulseAnimation(context)) {
                onPulseStart(isBreak)
            }
            onTimerComplete()
        }
    }

    val totalMillis = if (isBreak) breakDurationMinutes * 60 * 1000L
    else selectedFocusMinutes * 60 * 1000L

    val progress = if (totalMillis > 0) timeLeftMillis.toFloat() / totalMillis.toFloat() else 1f

    // 标尺显示当前阶段时长（专注=专注时长，休息=休息时长），避免与计时不一致
    val rulerValue = if (isBreak) breakDurationMinutes else selectedFocusMinutes
    LaunchedEffect(Unit) { createNotificationChannel(context) }

    DisposableEffect(isRunning) {
        view.keepScreenOn = isRunning
        onDispose {
            view.keepScreenOn = false
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val breakColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 今日完成数（仅专注时显示）
            if (!isBreak) {
                Text(
                    text = pluralStringResource(R.plurals.tomato_today_completed, todayCompletedPomodoros, todayCompletedPomodoros),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 正弦波环 + 时间
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                val activeColor = if (isBreak) breakColor else primaryColor
                val infiniteTransition = rememberInfiniteTransition()
                val wavePhase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val baseRadius = (size.minDimension / 2f) - strokeWidth * 2
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val numWaves = 10f
                    val waveAmplitude = 6.dp.toPx() * progress

                    // 背景圆 — 极淡
                    drawCircle(
                        color = activeColor.copy(alpha = 0.06f),
                        radius = baseRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = strokeWidth)
                    )

                    // 正弦波环
                    val path = Path()
                    val steps = 360
                    for (deg in 0..steps) {
                        val angle = Math.toRadians(deg.toDouble())
                        val wave = kotlin.math.sin(Math.toRadians((numWaves * deg + wavePhase).toDouble()))
                        val r = baseRadius + (wave * waveAmplitude).toFloat()
                        val x = centerX + r * kotlin.math.cos(angle).toFloat()
                        val y = centerY + r * kotlin.math.sin(angle).toFloat()
                        if (deg == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()

                    drawPath(
                        path = path,
                        color = activeColor.copy(alpha = 0.25f),
                        style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBreak) stringResource(R.string.tomato_break) else stringResource(R.string.tomato_focus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isBreak) breakColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimeMillis(timeLeftMillis),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 刻度尺 — 固定高度占位，运行时置灰不可点击
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .alpha(if (isRunning) 0.3f else 1f)
            ) {
                MinuteRulerPicker(
                    value = rulerValue,
                    onValueChange = { minutes ->
                        if (isBreak) {
                            breakDurationMinutes = minutes
                            timeLeftMillis = minutes * 60 * 1000L
                        } else {
                            selectedFocusMinutes = minutes
                            timeLeftMillis = minutes * 60 * 1000L
                        }
                    },
                    range = 3..180,
                    enabled = !isRunning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 控制按钮（专注结束后等待"开始休息"时隐藏）
            if (!awaitingBreakStart) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        TomatoRingtonePlayer.stop()
                        TomatoTimerState.reset()
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.tomato_reset),
                        modifier = Modifier.size(24.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        TomatoRingtonePlayer.stop()
                        if (isRunning) {
                            TomatoTimerState.pause()
                        } else {
                            TomatoTimerState.start(context)
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when {
                            isRunning -> if (isBreak) breakColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.35f)
                            else -> if (isBreak) breakColor else primaryColor
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) stringResource(R.string.tomato_pause) else stringResource(R.string.tomato_start),
                        modifier = Modifier.size(40.dp)
                    )
                }

                if (isBreak) {
                    FilledTonalIconButton(
                        onClick = {
                            TomatoRingtonePlayer.stop()
                            TomatoTimerState.skipBreak()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.tomato_skip_break),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(52.dp))
                }
            }
            }

            // 专注结束后：等待用户点击"开始休息"
            if (awaitingBreakStart) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        TomatoRingtonePlayer.stop()
                        TomatoTimerState.start(context)
                        view.keepScreenOn = true
                    },
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.tomato_start_break))
                }
            }
        }
    }
}

@Composable
private fun MinuteRulerPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: IntRange = 3..180,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    val scaleIntervalDp = 16.dp
    val scaleInterval = with(density) { scaleIntervalDp.toPx() }
    val bigScaleHeight = with(density) { 20.dp.toPx() }
    val smallScaleHeight = with(density) { 12.dp.toPx() }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val totalSteps = range.last - range.first

    fun clampOffset(raw: Float): Float {
        val minOff = -(totalSteps * scaleInterval)
        return raw.coerceIn(minOff, 0f)
    }

    var measuredWidth by remember { mutableFloatStateOf(0f) }
    var measuredHeight by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableFloatStateOf(-((value - range.first) * scaleInterval)) }

    // 外部 value 变化时（如专注↔休息切换）标尺重新居中对齐
    LaunchedEffect(value) {
        offset = -((value - range.first) * scaleInterval).toFloat()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    measuredWidth = it.width.toFloat()
                    measuredHeight = it.height.toFloat()
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset = clampOffset(offset + dragAmount.x)
                        val rawIndex = (-offset / scaleInterval).roundToInt()
                        val newIndex = rawIndex.coerceIn(0, totalSteps)
                        val newValue = newIndex + range.first
                        if (newValue != value) onValueChange(newValue)
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            for (i in range) {
                val index = i - range.first
                val x = centerX + offset + index * scaleInterval
                if (x < -scaleInterval * 3 || x > size.width + scaleInterval * 3) continue

                val normalizedDist = (kotlin.math.abs(x - centerX) / size.width).coerceIn(0f, 1f)
                val alpha = (1f - normalizedDist * normalizedDist).coerceIn(0.08f, 1f)

                val isCenter = i == value
                val isBigScale = i % 5 == 0

                val lineHeight = when {
                    isCenter -> bigScaleHeight
                    isBigScale -> bigScaleHeight * 0.7f
                    else -> smallScaleHeight
                }

                val lineWidth = when {
                    isCenter -> 2.dp.toPx()
                    isBigScale -> 1.5.dp.toPx()
                    else -> 1.dp.toPx()
                }

                drawLine(
                    color = onSurfaceColor.copy(alpha = if (isCenter) 1f else alpha * 0.6f),
                    start = Offset(x, centerY - lineHeight / 2f),
                    end = Offset(x, centerY + lineHeight / 2f),
                    strokeWidth = lineWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        if (measuredWidth > 0f && measuredHeight > 0f) {
            val centerX = measuredWidth / 2f
            val centerY = measuredHeight / 2f
            for (i in range) {
                val index = i - range.first
                val xPx = centerX + offset + index * scaleInterval
                if (xPx < -scaleInterval * 3 || xPx > measuredWidth + scaleInterval * 3) continue

                val isCenter = i == value
                val isBigScale = i % 5 == 0
                if (!isCenter && !isBigScale) continue

                val normalizedDist = (kotlin.math.abs(xPx - centerX) / measuredWidth).coerceIn(0f, 1f)
                val alpha = (1f - normalizedDist * normalizedDist).coerceIn(0.08f, 1f)

                val lineHeight = when {
                    isCenter -> bigScaleHeight
                    isBigScale -> bigScaleHeight * 0.7f
                    else -> smallScaleHeight
                }

                val textWidthDp = if (isCenter) 48.dp else 32.dp
                val textHeight = if (isCenter) 28.dp else 20.dp

                val yPx = if (isCenter) {
                    centerY - lineHeight / 2f - with(density) { 16.dp.toPx() + textHeight.toPx() }
                } else {
                    centerY + lineHeight / 2f + with(density) { 8.dp.toPx() }
                }

                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (xPx - with(density) { textWidthDp.toPx() / 2f }).toInt(),
                            y = yPx.toInt()
                        )
                    }
                        .width(textWidthDp)
                        .height(textHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$i",
                        fontSize = if (isCenter) 22.sp else 13.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = onSurfaceColor.copy(alpha = if (isCenter) 1f else alpha * 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatTimeMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun createNotificationChannel(context: Context) {
    val vibrate = PreferencesManager.getTomatoVibrate(context)
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        context.getString(R.string.tomato_timer_title),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.tomato_focus_complete_title)
        enableVibration(vibrate)
        if (vibrate) {
            vibrationPattern = longArrayOf(0, 300, 200, 300)
        }
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

/**
 * 更新通知渠道的震动设置（设置弹窗切换震动开关时调用）
 */
fun updateVibrationSetting(context: Context, enabled: Boolean) {
    createNotificationChannel(context)
}

/**
 * 番茄结束震动（独立于通知，直接调用系统 Vibrator，更可靠）
 */
private fun vibrate(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            // 震动3次：每次为「300ms 震 + 200ms 停 + 300ms 震」，共3组
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 200, 300, 200, 300, 200, 300, 200, 300, 200, 300),
                    -1
                )
            )
        }
    } catch (e: Exception) {
        Logger.e(TAG, "番茄结束震动失败: ${e.message}")
    }
}

private fun showTimerNotification(context: Context, timeLeftMillis: Long, isBreak: Boolean) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(if (isBreak) context.getString(R.string.tomato_break) else context.getString(R.string.tomato_focus))
        .setContentText(formatTimeMillis(timeLeftMillis))
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
    context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
}

private fun updateNotification(context: Context, timeLeftMillis: Long, isBreak: Boolean) {
    showTimerNotification(context, timeLeftMillis, isBreak)
}

private fun showCompletionNotification(context: Context, isBreak: Boolean) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val vibrateEnabled = PreferencesManager.getTomatoVibrate(context)
    val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(if (isBreak) context.getString(R.string.tomato_break_complete_title) else context.getString(R.string.tomato_focus_complete_title))
        .setContentText(if (isBreak) context.getString(R.string.tomato_break_complete_desc) else context.getString(R.string.tomato_focus_complete_desc))
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
    if (vibrateEnabled) {
        notificationBuilder.setVibrate(longArrayOf(0, 300, 200, 300))
    }
    val notification = notificationBuilder.build()
    context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
}

private fun cancelNotification(context: Context) {
    context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
}
