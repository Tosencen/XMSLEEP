package org.xmsleep.app.ui.breathing

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.service.BreathingService
import org.xmsleep.app.utils.Logger
import java.util.Locale

// 从 Context 中查找 Activity
private fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

/**
 * 屏幕常亮 Composable - Compose 风格
 * 当 condition 为 true 时保持屏幕常亮，为 false 时恢复默认
 */
@Composable
private fun KeepScreenOn(
    condition: Boolean, 
    activity: android.app.Activity?
) {
    LaunchedEffect(condition, activity) {
        activity?.window?.let { window ->
            if (condition) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

/**
 * 呼吸练习详情页面 - 提供呼吸引导功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingDetailScreen(
    methodId: String,
    activity: android.app.Activity? = null,
    onBack: () -> Unit,
    viewModel: BreathingViewModel = viewModel()
) {
    val currentMethod by viewModel.currentMethod.collectAsStateWithLifecycle()
    val durationMinutes by viewModel.durationMinutes.collectAsStateWithLifecycle()
    val isBreathing by viewModel.isActive.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    
    var breathPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var breathCount by remember { mutableIntStateOf(0) }
    var phaseCountdown by remember { mutableIntStateOf(0) }
    var showDurationSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化选择呼吸法
    LaunchedEffect(methodId) {
        val method = BreathingMethods.getMethods(context).find { it.id == methodId }
        if (method != null) {
            viewModel.selectMethod(method)
        }
    }
    
    // 监听页面离开 - 切换到其他页面时完全停止呼吸功能，并清除屏幕常亮
    DisposableEffect(Unit) {
        onDispose {
            // 清除屏幕常亮
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // 停止呼吸功能
            if (isBreathing) {
                viewModel.stopBreathing()
                // 停止后台服务
                val stopIntent = Intent(context, BreathingService::class.java).apply {
                    action = BreathingService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
    }
    
    // 教程弹窗状态
    var showTutorialDialog by remember { mutableStateOf(false) }
    
    // 常亮设置弹窗状态
    var showScreenOnDialog by remember { mutableStateOf(false) }
    var keepScreenOn by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getKeepScreenOn(context)) 
    }
    
    // 倒计时相关
    val totalTime = durationMinutes * 60 * 1000L
    val timeLeft = remainingSeconds * 1000L
    
    // MediaPlayer 用于播放呼吸声音
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    
    // 倒计时逻辑 - 使用 ViewModel 的状态
    LaunchedEffect(isBreathing) {
        if (isBreathing) {
            // 启动后台服务
            val serviceIntent = Intent(context, BreathingService::class.java).apply {
                putExtra("total_time", remainingSeconds * 1000L)
            }
            context.startForegroundService(serviceIntent)
        } else {
            // 停止服务
            val stopIntent = Intent(context, BreathingService::class.java).apply {
                action = BreathingService.ACTION_STOP
            }
            context.startService(stopIntent)
        }
    }
    
    // 初始化 MediaPlayer
    DisposableEffect(context) {
        mediaPlayer = MediaPlayer()
        onDispose {
            mediaPlayer?.release()
        }
    }
    
    // 播放呼吸声音
    LaunchedEffect(breathPhase, isBreathing) {
        if (isBreathing) {
            when (breathPhase) {
                BreathPhase.INHALE -> {
                    // 播放吸气声
                    mediaPlayer?.reset()
                    context.assets.openFd("breathing/breath-in.ogg").use { afd ->
                        mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                }
                BreathPhase.EXHALE -> {
                    // 播放呼气声
                    mediaPlayer?.reset()
                    context.assets.openFd("breathing/breath-out.ogg").use { afd ->
                        mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                }
                else -> {
                    // 屏息时停止声音
                    mediaPlayer?.pause()
                }
            }
        }
    }
    
    // 屏幕常亮 - Compose 风格
    KeepScreenOn(condition = isBreathing && keepScreenOn, activity = activity)
    
    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentMethod.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.flip_clock_back)
                        )
                    }
                },
                actions = {
                    // 常亮设置按钮
                    IconButton(onClick = { showScreenOnDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = context.getString(R.string.keep_screen_on),
                            tint = if (keepScreenOn) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 教程按钮
                    IconButton(onClick = { showTutorialDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = context.getString(R.string.tutorial),
                            tint = if (keepScreenOn) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        // 主内容区域 - 偏上居中
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
                Spacer(modifier = Modifier.height(80.dp))
                
                // 计数文字
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBreathing) {
                        Text(
                            text = context.getString(R.string.breath_count, breathCount + 1),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                // 呼吸引导动画（圆圈和中间文字）
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BreathingCircle(
                        isBreathing = isBreathing,
                        breathPhase = breathPhase,
                        currentMethod = currentMethod,
                        circleDiameter = 280.dp
                    )
                    
                    // 指导文字（放在圆的中间位置）
                    val guidanceText = when {
                        breathPhase == BreathPhase.INHALE -> context.getString(R.string.inhale).removeSuffix("...")
                        breathPhase == BreathPhase.HOLD -> context.getString(R.string.hold).removeSuffix("...")
                        breathPhase == BreathPhase.EXHALE -> context.getString(R.string.exhale).removeSuffix("...")
                        breathPhase == BreathPhase.HOLD_AFTER -> context.getString(R.string.hold_after).removeSuffix("...")
                        else -> ""
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = guidanceText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (isBreathing && phaseCountdown > 0) {
                            Text(
                                text = "${phaseCountdown}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 按钮区域：时长图标按钮 + 开始/停止按钮（竖排）
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 时长图标按钮 - 显示倒计时
                    FilledTonalButton(
                        onClick = { showDurationSelector = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(160.dp)
                            .height(48.dp)
                    ) {
                        if (isBreathing && remainingSeconds > 0) {
                            // 倒计时显示
                            val minutes = remainingSeconds / 60
                            val seconds = remainingSeconds % 60
                            Text(
                                text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = context.getString(R.string.custom_duration),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // 开始/停止按钮
                    Button(
                        onClick = { 
                            if (isBreathing) {
                                viewModel.stopBreathing()
                            } else {
                                viewModel.startBreathing()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .width(160.dp)
                            .height(64.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 文字
                            Text(
                                text = if (isBreathing) context.getString(R.string.stop) else context.getString(R.string.play),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            
                            // 进度条（只在开始时显示，底部对齐）
                            if (isBreathing && totalTime > 0) {
                                LinearProgressIndicator(
                                    progress = { (totalTime - timeLeft).toFloat() / totalTime.toFloat() },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .offset(y = (-2).dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
    }
    
    // 时长选择弹窗
    if (showDurationSelector) {
        DurationDialog(
            selectedMinutes = durationMinutes,
            onDurationChange = { viewModel.setDuration(it) },
            onDismiss = { showDurationSelector = false }
        )
    }
    
    // 教程弹窗
    if (showTutorialDialog) {
        BreathingMethodTutorialDialog(
            method = currentMethod,
            onDismiss = { showTutorialDialog = false }
        )
    }
    
    // 常亮设置弹窗
    if (showScreenOnDialog) {
        ScreenOnSettingsDialog(
            keepScreenOn = keepScreenOn,
            onKeepScreenOnChange = { 
                keepScreenOn = it
                org.xmsleep.app.preferences.PreferencesManager.saveKeepScreenOn(context, it)
            },
            onDismiss = { showScreenOnDialog = false }
        )
    }
    
    // 呼吸循环逻辑
    LaunchedEffect(isBreathing, currentMethod) {
        if (isBreathing) {
            breathCount = 0
            while (isBreathing) {
                // 吸气
                breathPhase = BreathPhase.INHALE
                for (i in currentMethod.inhale downTo 1) {
                    phaseCountdown = i
                    delay(1000L)
                }
                
                // 屏息（如果有的话）
                if (currentMethod.hold > 0) {
                    breathPhase = BreathPhase.HOLD
                    for (i in currentMethod.hold downTo 1) {
                        phaseCountdown = i
                        delay(1000L)
                    }
                }
                
                // 呼气
                breathPhase = BreathPhase.EXHALE
                for (i in currentMethod.exhale downTo 1) {
                    phaseCountdown = i
                    delay(1000L)
                }
                
                // 呼气后屏息（箱式呼吸）
                if (currentMethod.holdAfter > 0) {
                    breathPhase = BreathPhase.HOLD_AFTER
                    for (i in currentMethod.holdAfter downTo 1) {
                        phaseCountdown = i
                        delay(1000L)
                    }
                }
                
                breathCount++
            }
        } else {
            breathPhase = BreathPhase.INHALE
            phaseCountdown = 0
        }
    }
}

private enum class BreathPhase {
    INHALE,      // 吸气
    HOLD,        // 屏息
    EXHALE,      // 呼气
    HOLD_AFTER   // 呼气后屏息（箱式呼吸）
}

@Composable
private fun DurationDialog(
    selectedMinutes: Int,
    onDurationChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val presetOptions = listOf(2, 5, 8, 10, 15, 20)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.select_duration),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // 预设按钮
                Text(
                    text = context.getString(R.string.preset_duration),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第一行：3个
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.take(3).forEach { minutes ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = { onDurationChange(minutes) },
                            label = { Text(context.resources.getQuantityString(R.plurals.minutes, minutes, minutes)) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第二行：3个
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetOptions.drop(3).forEach { minutes ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = { onDurationChange(minutes) },
                            label = { Text(context.resources.getQuantityString(R.plurals.minutes, minutes, minutes)) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 滑杆
                Text(
                    text = context.getString(R.string.custom_duration),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = context.resources.getQuantityString(R.plurals.minutes, selectedMinutes, selectedMinutes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { onDurationChange(it.toInt()) },
                    valueRange = 0f..60f,
                    steps = 11
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.done))
            }
        }
    )
}

@Composable
private fun BreathingCircle(
    isBreathing: Boolean,
    breathPhase: BreathPhase,
    currentMethod: BreathingMethod,
    circleDiameter: Dp = 200.dp
) {
    // 根据呼吸阶段确定圆圈大小
    val targetScale = when (breathPhase) {
        BreathPhase.INHALE -> 1.0f
        BreathPhase.HOLD -> 1.0f
        BreathPhase.EXHALE -> 0.5f
        BreathPhase.HOLD_AFTER -> 0.5f
    }
    
    // 动画时长根据当前阶段动态调整
    val animationDuration = when (breathPhase) {
        BreathPhase.INHALE -> currentMethod.inhale * 1000
        BreathPhase.HOLD -> currentMethod.hold * 1000
        BreathPhase.EXHALE -> currentMethod.exhale * 1000
        BreathPhase.HOLD_AFTER -> currentMethod.holdAfter * 1000
    }.coerceAtLeast(100) // 最小100ms
    
    val scale by animateFloatAsState(
        targetValue = if (isBreathing) targetScale else 0.5f,
        animationSpec = tween(
            durationMillis = if (isBreathing) animationDuration else 500,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )
    
    val animatedSize = circleDiameter * scale
    
    // 外层圆圈使用深色（透明度 0.1），内层圆圈透明度 0.2
    val outerCircleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val innerCircleColor = MaterialTheme.colorScheme.background.copy(alpha = 0.2f)
    
    Box(
        modifier = Modifier.size(circleDiameter),
        contentAlignment = Alignment.Center
    ) {
        // 外层圆圈（固定大小，深色）
        Box(
            modifier = Modifier
                .size(circleDiameter)
                .clip(CircleShape)
                .background(outerCircleColor)
        )
        
        // 内层呼吸圆圈（缩放效果，透明度 0.2）
        Box(
            modifier = Modifier
                .size(animatedSize)
                .clip(CircleShape)
                .background(innerCircleColor)
        )
    }
}

@Composable
private fun DurationSelector(
    selectedMinutes: Int,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presetOptions = listOf(2, 5, 10, 15)
    
    Column(modifier = modifier) {
        // 预设按钮
        Text(
            text = context.getString(R.string.preset_duration),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetOptions.forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onDurationChange(minutes) },
                    label = { Text(context.resources.getQuantityString(R.plurals.minutes, minutes, minutes)) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 滑杆
        Text(
            text = context.getString(R.string.custom_duration),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = context.resources.getQuantityString(R.plurals.minutes, selectedMinutes, selectedMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Slider(
            value = selectedMinutes.toFloat(),
            onValueChange = { onDurationChange(it.toInt()) },
            valueRange = 0f..60f,
            steps = 59
        )
    }
}

@Composable
private fun BreathingMethodTutorialDialog(
    method: BreathingMethod,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = method.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 副标题
                Text(
                    text = method.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider()
                
                // 描述
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                // 操作步骤
                Text(
                    text = context.getString(R.string.operation_steps),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                method.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider()
                
                // 节奏信息
                Text(
                    text = context.getString(R.string.breathing_rhythm),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                val rhythmDetail = listOf(
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_inhale, method.inhale, method.inhale),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_hold, method.hold, method.hold),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_exhale, method.exhale, method.exhale),
                    context.resources.getQuantityString(R.plurals.breathing_rhythm_hold, method.holdAfter, method.holdAfter)
                ).joinToString(" → ")
                Text(
                    text = rhythmDetail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.got_it))
            }
        }
    )
}

@Composable
private fun ScreenOnSettingsDialog(
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = context.getString(R.string.keep_screen_on),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = context.getString(R.string.screen_on_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 开关控件
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = context.getString(R.string.keep_screen_on),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = onKeepScreenOnChange
                    )
                }
                
                // 状态提示
                Text(
                    text = if (keepScreenOn) {
                        context.getString(R.string.screen_on_enabled)
                    } else {
                        context.getString(R.string.screen_on_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (keepScreenOn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(R.string.done))
            }
        }
    )
}
