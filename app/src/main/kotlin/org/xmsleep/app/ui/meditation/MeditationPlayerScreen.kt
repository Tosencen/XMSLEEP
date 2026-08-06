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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
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
import androidx.compose.ui.res.painterResource
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.meditation.BilibiliAudioHelper
import org.xmsleep.app.meditation.MeditationPlayerManager
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

    val manifest = remember {
        loadMeditationManifest(context)
    }

    val category = remember(categoryId) {
        manifest?.categories?.find { it.id == categoryId }
    }

    val sessions = remember(categoryId) {
        manifest?.sessions?.filter { it.category == categoryId } ?: emptyList()
    }

    // 从管理器同步状态（仅同步属于当前分类的会话，避免跨分类误跳转）
    val managerIsPlaying by playerManager.isPlaying.collectAsState()
    val managerSessionId by playerManager.currentSessionId.collectAsState()
    val managerCategoryId by playerManager.currentCategoryId.collectAsState()
    val isLoop by playerManager.isLoop.collectAsState()
    val repeatCount by playerManager.repeatCount.collectAsState()

    var currentSessionIndex by remember {
        mutableIntStateOf(
            if (sessionId != null) {
                sessions.indexOfFirst { it.id == sessionId }.coerceAtLeast(0)
            } else 0
        )
    }

    // 始终同步管理器中的播放会话到本地（修复语言切换后状态丢失）
    LaunchedEffect(managerSessionId, managerCategoryId, sessions) {
        if (managerSessionId != null && managerCategoryId == categoryId && sessions.isNotEmpty()) {
            val index = sessions.indexOfFirst { it.id == managerSessionId }
            if (index >= 0 && index != currentSessionIndex) {
                currentSessionIndex = index
            }
        }
    }

    val currentSession = sessions.getOrNull(currentSessionIndex)

    // 当前播放的会话是否属于本分类
    val isManagerPlayingThisCategory = managerIsPlaying && managerCategoryId == categoryId
    // 正在播放的会话是否为当前显示的会话
    val isCurrentSessionPlaying = isManagerPlayingThisCategory && managerSessionId == currentSession?.id

    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentTime by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
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
            if (playerManager.resume(context)) return
            // resume失败（错误状态），继续走到完整play()路径重新获取URL
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
                        playerManager.play(context, categoryId, session.id, audioUrl, session.getLocalizedName(languageCode))
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

            // 冥想图标区域（涟漪在外层，呼吸缩放直接作用于图标，无圆背景）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                if (isCurrentSessionPlaying) {
                    RippleAnimation()
                }

                // 冥想图标（呼吸效果，带轻微透明度）
                BreathingCircle(
                    isPlaying = isCurrentSessionPlaying,
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = context.getString(R.string.meditation),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
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

            // 操作行：左=循环，中=播放/暂停，右=倒计时
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左：循环播放（选中时图标变主题色，背景样式参考电台按钮）
                Surface(
                    onClick = { playerManager.toggleLoop() },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLoop) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.repeat_24),
                            contentDescription = context.getString(R.string.loop_play),
                            tint = if (isLoop) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 中：播放/暂停按钮（圆角与循环/倒计时一致）
                Surface(
                    onClick = {
                        if (!isLoading) togglePlayPause()
                    },
                    modifier = Modifier
                        .width(140.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
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

                // 右：循环次数（×1/×2/×3，播满N遍自动停止；与左侧无限循环互斥）
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = { playerManager.cycleRepeatCount() },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (repeatCount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RepeatOne,
                                contentDescription = if (repeatCount > 0) {
                                    context.getString(R.string.meditation_repeat_count, repeatCount)
                                } else {
                                    context.getString(R.string.meditation_repeat_times)
                                },
                                tint = if (repeatCount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (repeatCount > 0) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Text(
                                text = "×$repeatCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
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
    ) {
        content()
    }
}

@Composable
private fun RippleAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    // 单个圆：从中心向外扩散并透明消失，消失后自动重头开始（始终只有一个圆）
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0f at 0 with FastOutSlowInEasing
                0.3f at 600 with FastOutSlowInEasing
                0f at 2000 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    val rippleColor = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = rippleScale
                    scaleY = rippleScale
                    alpha = rippleAlpha
                }
                .clip(CircleShape)
                .background(rippleColor.copy(alpha = 1f))
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
        context.assets.open("meditation_remote.json").use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                val type = object : TypeToken<MeditationManifest>() {}.type
                Gson().fromJson(reader, type)
            }
        }
    } catch (e: Exception) {
        null
    }
}
