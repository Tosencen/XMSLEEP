package org.xmsleep.app.ui.meditation

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.meditation.BilibiliAudioHelper
import org.xmsleep.app.meditation.MeditationPlayerManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.ui.TimerDialog
import org.xmsleep.app.ui.components.PlayingAnimation
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationPlayerScreen(
    categoryId: String,
    sessionId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val languageCode = LanguageManager.getCurrentLanguage(context).code
    val scope = rememberCoroutineScope()
    val playerManager = remember { MeditationPlayerManager.getInstance() }
    val timerManager = remember { TimerManager.getInstance() }

    val manifest = remember {
        loadMeditationManifest(context)
    }

    val category = remember(categoryId) {
        manifest?.categories?.find { it.id == categoryId }
    }

    val sessions = remember(categoryId) {
        manifest?.sessions?.filter { it.category == categoryId } ?: emptyList()
    }

    var currentSessionIndex by remember {
        mutableIntStateOf(
            if (sessionId != null) {
                sessions.indexOfFirst { it.id == sessionId }.coerceAtLeast(0)
            } else 0
        )
    }

    val currentSession = sessions.getOrNull(currentSessionIndex)

    // 从管理器同步状态（仅同步属于当前分类的会话，避免跨分类误跳转）
    val managerIsPlaying by playerManager.isPlaying.collectAsState()
    val managerSessionId by playerManager.currentSessionId.collectAsState()
    val managerCategoryId by playerManager.currentCategoryId.collectAsState()
    val isLoop by playerManager.isLoop.collectAsState()
    val timerActive by timerManager.isTimerActive.collectAsState()
    val timerLeft by timerManager.timeLeftMillis.collectAsState()

    // 当前播放的会话是否属于本分类
    val isManagerPlayingThisCategory = managerIsPlaying && managerCategoryId == categoryId
    // 正在播放的会话是否为当前显示的会话
    val isCurrentSessionPlaying = isManagerPlayingThisCategory && managerSessionId == currentSession?.id

    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    // 用户主动切换会话后，待自动播放标记
    var pendingAutoPlay by remember { mutableStateOf(false) }

    // 点击播放/暂停（定义在使用它的 LaunchedEffect 之前）
    fun togglePlayPause() {
        val isCurrentSession = managerSessionId == currentSession?.id

        if (isCurrentSessionPlaying) {
            playerManager.pause()
            return
        }

        if (!managerIsPlaying && isCurrentSession && playerManager.hasMedia()) {
            playerManager.resume(context)
            return
        }

        currentSession?.let { session ->
            scope.launch {
                isLoading = true
                try {
                    val audioUrl = withContext(Dispatchers.IO) {
                        if (session.audioUrl.isNotEmpty()) {
                            session.audioUrl
                        } else if (session.sourceId.isNotEmpty()) {
                            BilibiliAudioHelper.getAudioUrl(session.sourceId)
                        } else {
                            null
                        }
                    }
                    if (audioUrl != null) {
                        playerManager.play(context, categoryId, session.id, audioUrl)
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // 初始化管理器
    LaunchedEffect(Unit) {
        playerManager.initialize(context)
    }

    // 同步管理器的会话到本地 currentSessionIndex（仅当播放的会话属于本分类时）
    LaunchedEffect(managerSessionId, managerCategoryId) {
        if (managerSessionId != null && managerCategoryId == categoryId) {
            val index = sessions.indexOfFirst { it.id == managerSessionId }
            if (index >= 0 && index != currentSessionIndex) {
                currentSessionIndex = index
            }
        }
    }

    // 切换音频时重置进度（总时长先用 manifest 里的真实值兜底，READY 后由播放器修正）
    LaunchedEffect(currentSession) {
        currentSession?.let { session ->
            if (managerSessionId != session.id) {
                currentTime = 0L
                progress = 0f
                duration = (session.duration * 1000L).coerceAtLeast(1L)
            }
        }
        // 用户主动切换会话后自动播放新音频
        if (pendingAutoPlay) {
            pendingAutoPlay = false
            togglePlayPause()
        }
    }

    // 更新播放进度（拖动时暂停自动更新，避免与用户操作冲突）
    LaunchedEffect(isCurrentSessionPlaying) {
        while (isCurrentSessionPlaying) {
            if (!isDragging) {
                currentTime = playerManager.getCurrentPosition()
                val dur = playerManager.getDuration()
                // 优先使用播放器真实时长，未就绪时用 manifest 兜底
                duration = if (dur > 0) dur else (currentSession?.duration?.times(1000L) ?: 1L).coerceAtLeast(1L)
                progress = (currentTime.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            delay(100L)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category?.getLocalizedName(languageCode) ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 冥想图标区域（涟漪在外层，圆形背景呼吸缩放）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                if (isCurrentSessionPlaying) {
                    RippleAnimation()
                }

                // 圆形背景 + 冥想图标（呼吸效果）
                BreathingCircle(
                    isPlaying = isCurrentSessionPlaying,
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = context.getString(R.string.meditation),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = currentSession?.getLocalizedName(languageCode) ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${formatTime(currentTime)} / ${formatTime(duration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条上方操作行：左上角循环按钮，右上角倒计时按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左上角：循环播放（选中时图标变主题色，无圆形背景）
                IconButton(
                    onClick = { playerManager.toggleLoop() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = context.getString(R.string.loop_play),
                        tint = if (isLoop) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 右上角：倒计时（与白噪音页区分：沙漏图标 + 内联，复用全局 TimerManager 不冲突）
                Box(contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { showTimerDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (timerActive) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = context.getString(R.string.set_countdown),
                            tint = if (timerActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (timerActive && timerLeft > 0) {
                        val minutes = (timerLeft / 60000).toInt()
                        val seconds = ((timerLeft % 60000) / 1000).toInt()
                        val badgeText = if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')}"
                        else "${seconds}s"
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // 波浪进度条
            me.saket.squiggles.SquigglySlider(
                value = progress,
                onValueChange = { newValue ->
                    isDragging = true
                    progress = newValue
                    currentTime = (newValue * duration).toLong()
                },
                onValueChangeFinished = {
                    isDragging = false
                    playerManager.seekTo((progress * duration).toLong())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 播放/暂停按钮（长方形，图标+文字）
            Surface(
                onClick = {
                    if (!isLoading) togglePlayPause()
                },
                modifier = Modifier
                    .width(180.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primary,
                enabled = true
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isCurrentSessionPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCurrentSessionPlaying) context.getString(R.string.pause) else context.getString(R.string.play),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 音频列表
            if (sessions.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.select_audio),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                LazyRow(
                    state = rememberLazyListState(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(sessions) { index, session ->
                        val isSelected = index == currentSessionIndex
                        MeditationAudioCard(
                            session = session,
                            languageCode = languageCode,
                            isSelected = isSelected,
                            onClick = {
                                if (index != currentSessionIndex) {
                                    playerManager.stop()
                                    currentSessionIndex = index
                                    pendingAutoPlay = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 倒计时设置对话框（复用全局 TimerManager，与白噪音页共享同一倒计时，避免冲突）
    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onTimerSet = { minutes ->
                if (minutes > 0) {
                    if (playerManager.hasMedia()) {
                        timerManager.startTimer(minutes)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.countdown_set_minutes, minutes),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.please_play_sound_before_timer),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    timerManager.cancelTimer()
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.countdown_cancelled),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                showTimerDialog = false
            },
            currentTimerMinutes = if (timerActive) timerManager.getCurrentTimerMinutes() else 0
        )
    }
}

@Composable
private fun BreathingCircle(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    // 呼吸效果：缩放 + 透明度，缓慢循环
    val scale by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.92f else 1f,
        targetValue = if (isPlaying) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isPlaying) 0.75f else 1f,
        targetValue = if (isPlaying) 1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
    ) {
        content()
    }
}

@Composable
private fun RippleAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha1"
    )

    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 667, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )
    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 667, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha2"
    )

    val rippleScale3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1333, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple3"
    )
    val rippleAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1333, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha3"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = rippleScale1
                    scaleY = rippleScale1
                    alpha = rippleAlpha1
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = rippleScale2
                    scaleY = rippleScale2
                    alpha = rippleAlpha2
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = rippleScale3
                    scaleY = rippleScale3
                    alpha = rippleAlpha3
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun MeditationAudioCard(
    session: MeditationSession,
    languageCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier
            .width(150.dp)
            .height(80.dp),
        shape = RoundedCornerShape(12.dp),
        color = cardColor,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = session.getLocalizedName(languageCode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Text(
                text = session.durationText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun loadMeditationManifest(context: Context): MeditationManifest? {
    return try {
        val inputStream = context.assets.open("meditation_remote.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<MeditationManifest>() {}.type
        Gson().fromJson(reader, type)
    } catch (e: Exception) {
        null
    }
}
