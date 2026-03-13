package org.xmsleep.app.ui.breathing

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.LightMode
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
 * 呼吸练习页面 - 提供呼吸引导功能
 */
@Composable
fun BreathingScreen(
    modifier: Modifier = Modifier,
    activity: android.app.Activity? = null,
    viewModel: BreathingViewModel = viewModel()
) {
    val isBreathing by viewModel.isActive.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    
    var breathPhase by remember { mutableStateOf(BreathPhase.INHALE) }
    var breathCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 使用传入的 Activity 参数
    
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
    var showTutorialDialog by remember { mutableStateOf(false) } // 只在点击按钮时显示
    
    // 常亮设置弹窗状态
    var showScreenOnDialog by remember { mutableStateOf(false) }
    var keepScreenOn by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getKeepScreenOn(context)) 
    } // 从 SharedPreferences 加载设置
    
    // 倒计时相关
    val totalTime = 5 * 60 * 1000L // 5 分钟（毫秒）
    // 使用 ViewModel 的剩余秒数来显示进度
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
    // 当正在呼吸且开启常亮设置时，屏幕保持常亮
    KeepScreenOn(condition = isBreathing && keepScreenOn, activity = activity)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent) // 透明背景，显示应用背景
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部标题和常亮按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = context.getString(R.string.breathing_exercise),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    // 常亮设置按钮
                    IconButton(
                        onClick = { showScreenOnDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = context.getString(R.string.keep_screen_on),
                            tint = if (keepScreenOn) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 教程按钮 - 颜色与常亮开关保持一致
                    IconButton(
                        onClick = { showTutorialDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = context.getString(R.string.tutorial),
                            tint = if (keepScreenOn) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center, // 居中排列
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-60).dp)
            ) {
                // 计数文字（固定占位，避免挤压）
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
                    circleDiameter = 280.dp
                )
                
                // 指导文字（放在圆的中间位置）
                val guidanceText = when {
                    breathPhase == BreathPhase.INHALE -> context.getString(R.string.inhale).removeSuffix("...")
                    breathPhase == BreathPhase.HOLD -> context.getString(R.string.hold).removeSuffix("...")
                    breathPhase == BreathPhase.EXHALE -> context.getString(R.string.exhale).removeSuffix("...")
                    else -> ""
                }
                
                Text(
                    text = guidanceText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
                Spacer(modifier = Modifier.height(64.dp))
                
                // 开始/停止按钮（矩形，图标 + 文字 + 进度条）
                Button(
                    onClick = { 
                        if (isBreathing) {
                            viewModel.stopBreathing()
                        } else {
                            viewModel.startBreathing(5 * 60) // 5 分钟
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 图标和文字
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = if (isBreathing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBreathing) context.getString(R.string.stop) else context.getString(R.string.play),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // 进度条（只在开始时显示，底部对齐）
                        if (isBreathing) {
                            LinearProgressIndicator(
                                progress = { (totalTime - timeLeft).toFloat() / totalTime.toFloat() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .offset(y = 2.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
        
        // 教程弹窗
        if (showTutorialDialog) {
            TutorialDialog(
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
    }
    
    // 呼吸循环逻辑
    LaunchedEffect(isBreathing) {
        if (isBreathing) {
            breathCount = 0
            while (isBreathing) {
                // 吸气 4 秒
                breathPhase = BreathPhase.INHALE
                delay(4000)
                
                // 屏息 4 秒
                breathPhase = BreathPhase.HOLD
                delay(4000)
                
                // 呼气 4 秒
                breathPhase = BreathPhase.EXHALE
                delay(4000)
                
                breathCount++
            }
        } else {
            breathPhase = BreathPhase.INHALE
        }
    }
}

private enum class BreathPhase {
    INHALE,   // 吸气
    HOLD,     // 屏息
    EXHALE    // 呼气
}

@Composable
private fun BreathingCircle(
    isBreathing: Boolean,
    breathPhase: BreathPhase,
    circleDiameter: Dp = 200.dp // 新增参数，支持自定义圆圈大小
) {
    // 根据呼吸阶段确定圆圈大小
    val targetScale = when (breathPhase) {
        BreathPhase.INHALE -> 1.0f
        BreathPhase.HOLD -> 1.0f
        BreathPhase.EXHALE -> 0.5f
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isBreathing) targetScale else 0.5f,
        animationSpec = tween(
            durationMillis = if (isBreathing) 4000 else 500,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )
    
    val animatedSize = circleDiameter * scale
    
    // 外层圆圈使用深色（透明度 0.1），内层圆圈透明度 0.2
    val outerCircleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // 降低透明度到 0.1
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
private fun TutorialDialog(onDismiss: () -> Unit) {
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
                text = context.getString(R.string.breathing_guide_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = context.getString(R.string.breathing_guide_step1),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = context.getString(R.string.breathing_guide_step2),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = context.getString(R.string.breathing_guide_step3),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = context.getString(R.string.breathing_guide_step4),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = context.getString(R.string.breathing_guide_step5),
                    style = MaterialTheme.typography.bodyLarge,
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
