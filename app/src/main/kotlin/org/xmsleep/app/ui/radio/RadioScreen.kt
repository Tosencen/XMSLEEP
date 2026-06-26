package org.xmsleep.app.ui.radio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.LottieValueCallback
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import org.xmsleep.app.R
import org.xmsleep.app.audio.model.RadioStation
import androidx.compose.ui.res.stringResource
import org.xmsleep.app.audio.BilibiliApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.ui.ThemeColorCallback
import org.xmsleep.app.ui.TimerDialog
import org.xmsleep.app.ui.TimerFAB
import org.xmsleep.app.ui.viewmodel.RadioViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    modifier: Modifier = Modifier,
    viewModel: RadioViewModel = viewModel(
        factory = RadioViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val radioPlayer = viewModel.radioPlayer
    val countdownTimer = viewModel.countdownTimer

    var showTimerDialog by remember { mutableStateOf(false) }

    val currentStation by viewModel.currentStation.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val timerTimeLeft by countdownTimer.timeLeftMillis.collectAsState()
    val isTimerActive by countdownTimer.isActive.collectAsState()

    // 导航栏适配（应用底部 Tab 栏）
    val bottomTabBarHeight = 112.dp // MainScreen 底部 Tab 栏高度: padding(bottom=32dp) + Row(height=80dp)
    val fabBottomPadding = bottomTabBarHeight + 24.dp

    // 首次进入自动播放（只在 ViewModel 初始化时触发，切 tab 不重复）
    LaunchedEffect(Unit) {
        viewModel.playIfNeeded()
    }

    val context = LocalContext.current

    var showVolumeDialog by remember { mutableStateOf(false) }
    var showBilibiliSheet by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val bilibiliRooms by viewModel.bilibiliRooms.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val playingRoomId by viewModel.playingRoomId.collectAsState()
    val lottieAnimationFile by viewModel.lottieAnimationFile.collectAsState()
    val pinnedRoomIds by viewModel.pinnedRoomIds.collectAsState()
    val pinnedRoomInfos by viewModel.pinnedRoomInfos.collectAsState()
    val playingRoomInfo by viewModel.playingRoomInfo.collectAsState()

    // Lottie 装饰动画（主题色适配）
    val colorScheme = MaterialTheme.colorScheme
    val backgroundHct = colorScheme.background.toHct()
    val isDark = backgroundHct.tone < 50.0
    val primaryHct = colorScheme.primary.toHct()
    val darkColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(35.0), if (isDark) 55.0 else 45.0).toInt())
    val mediumColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(32.0), if (isDark) 62.0 else 65.0).toInt())
    val lightColor = Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(30.0), if (isDark) 68.0 else 75.0).toInt())
    val backgroundColor = if (isDark) {
        Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(35.0), 28.0).toInt())
    } else {
        Color(Hct.from(primaryHct.hue, primaryHct.chroma.coerceAtMost(20.0), 96.0).toInt())
    }
    val secondaryHct = colorScheme.secondary.toHct()
    val secondaryColor = Color(Hct.from(secondaryHct.hue, secondaryHct.chroma.coerceAtMost(28.0), secondaryHct.tone).toInt())
    val darkGrayColor = if (isDark) colorScheme.onSurface.copy(alpha = 0.87f) else colorScheme.onSurface.copy(alpha = 0.6f)
    val mediumGrayColor = if (isDark) colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val lightGrayColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.5f) else colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 状态栏间距
            val statusBarTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
            Spacer(modifier = Modifier.height(statusBarTop + 8.dp))

            // 中间内容区 — 整体垂直居中
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 装饰动画（主题色适配）
                Box(
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    key(lottieAnimationFile) {
                        AndroidView(
                            factory = { ctx ->
                                LottieAnimationView(ctx).apply {
                                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                                    setAnimation(lottieAnimationFile)
                                    repeatCount = LottieDrawable.INFINITE
                                    isClickable = false
                                    isFocusable = false
                                    addLottieOnCompositionLoadedListener {
                                        if (lottieAnimationFile == "zx.lottie" || lottieAnimationFile == "dq.lottie") {
                                            addValueCallback(KeyPath("**"), LottieProperty.COLOR,
                                                ThemeColorCallback(darkColor.toArgb(), mediumColor.toArgb(), lightColor.toArgb(),
                                                    secondaryColor.toArgb(), backgroundColor.toArgb(), darkGrayColor.toArgb(),
                                                    mediumGrayColor.toArgb(), lightGrayColor.toArgb(), primaryHct, isDark))
                                            addValueCallback(KeyPath("**"), LottieProperty.STROKE_COLOR,
                                                ThemeColorCallback(darkColor.toArgb(), mediumColor.toArgb(), lightColor.toArgb(),
                                                    secondaryColor.toArgb(), backgroundColor.toArgb(), darkGrayColor.toArgb(),
                                                    mediumGrayColor.toArgb(), lightGrayColor.toArgb(), primaryHct, isDark))
                                        }
                                        if (lottieAnimationFile == "zx.lottie") {
                                            addValueCallback(KeyPath("Body front Outlines"), LottieProperty.COLOR, LottieValueCallback(lightGrayColor.toArgb()))
                                            addValueCallback(KeyPath("eyeR Outlines"), LottieProperty.COLOR, LottieValueCallback(lightGrayColor.toArgb()))
                                            addValueCallback(KeyPath("eyeL Outlines"), LottieProperty.COLOR, LottieValueCallback(lightGrayColor.toArgb()))
                                        }
                                        if (isPlaying) playAnimation() else pauseAnimation()
                                    }
                                }
                            },
                            update = { view ->
                                if (lottieAnimationFile == "zx.lottie" || lottieAnimationFile == "dq.lottie") {
                                    view.addValueCallback(
                                        KeyPath("**"),
                                        LottieProperty.COLOR,
                                        ThemeColorCallback(
                                            darkColor = darkColor.toArgb(),
                                            mediumColor = mediumColor.toArgb(),
                                            lightColor = lightColor.toArgb(),
                                            secondaryColor = secondaryColor.toArgb(),
                                            backgroundColor = backgroundColor.toArgb(),
                                            darkGrayColor = darkGrayColor.toArgb(),
                                            mediumGrayColor = mediumGrayColor.toArgb(),
                                            lightGrayColor = lightGrayColor.toArgb(),
                                            primaryHct = primaryHct,
                                            isDarkMode = isDark
                                        )
                                    )
                                    view.addValueCallback(
                                        KeyPath("**"),
                                        LottieProperty.STROKE_COLOR,
                                        ThemeColorCallback(
                                            darkColor = darkColor.toArgb(),
                                            mediumColor = mediumColor.toArgb(),
                                            lightColor = lightColor.toArgb(),
                                            secondaryColor = secondaryColor.toArgb(),
                                            backgroundColor = backgroundColor.toArgb(),
                                            darkGrayColor = darkGrayColor.toArgb(),
                                            mediumGrayColor = mediumGrayColor.toArgb(),
                                            lightGrayColor = lightGrayColor.toArgb(),
                                            primaryHct = primaryHct,
                                            isDarkMode = isDark
                                        )
                                    )
                                }
                                if (lottieAnimationFile == "zx.lottie") {
                                    view.addValueCallback(
                                        KeyPath("Body front Outlines"),
                                        LottieProperty.COLOR,
                                        LottieValueCallback(lightGrayColor.toArgb())
                                    )
                                    view.addValueCallback(
                                        KeyPath("eyeR Outlines"),
                                        LottieProperty.COLOR,
                                        LottieValueCallback(lightGrayColor.toArgb())
                                    )
                                    view.addValueCallback(
                                        KeyPath("eyeL Outlines"),
                                        LottieProperty.COLOR,
                                        LottieValueCallback(lightGrayColor.toArgb())
                                    )
                                }
                                view.invalidate()
                                if (isPlaying) {
                                    if (!view.isAnimating) view.playAnimation()
                                } else {
                                    view.pauseAnimation()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 电台名称
                Text(
                    text = stringResource(R.string.radio_station_noise),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentStation.tags.forEach { tagRes ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = stringResource(tagRes),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 播放/暂停按钮 + 菜单 + 音量控制
                val playInteractionSource = remember { MutableInteractionSource() }
                val isSquare = isPlaying || isBuffering

                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showVolumeDialog = true },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.volume),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val buttonWidth by animateDpAsState(
                        targetValue = if (isSquare) 100.dp else 120.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.5f),
                        label = "buttonWidth"
                    )
                    Button(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .width(buttonWidth)
                            .height(100.dp),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(0.dp),
                        interactionSource = playInteractionSource
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                strokeWidth = 5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                modifier = Modifier.size(if (isSquare) 60.dp else 44.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            showBilibiliSheet = true
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 底部安全区（系统导航栏 + 应用底部 Tab 栏）
            Spacer(modifier = Modifier.height(bottomTabBarHeight + 8.dp))
        }

        // FAB
        // 倒计时 FAB（同首页样式）
        TimerFAB(
            isTimerActive = isTimerActive,
            timeLeftMillis = timerTimeLeft,
            onClick = { showTimerDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = fabBottomPadding)
        )

        FloatingRadioBubble(
            visible = playingRoomId != null && isPlaying,
            room = playingRoomInfo,
            roomId = playingRoomId,
            isPlaying = isPlaying,
            viewModel = viewModel,
        )

    }

    // 倒计时对话框
    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onTimerSet = { minutes ->
                if (minutes > 0) {
                    countdownTimer.start(minutes)
                } else {
                    countdownTimer.cancel()
                }
                showTimerDialog = false
            },
            currentTimerMinutes = if (isTimerActive) countdownTimer.getCurrentMinutes() else 0
        )
    }

    // 音量调节对话框
    if (showVolumeDialog) {
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text(context.getString(R.string.volume)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = volume,
                        onValueChange = { viewModel.setVolume(it) },
                        modifier = Modifier.fillMaxWidth(),
                        valueRange = 0f..1f,
                        steps = 19
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVolumeDialog = false }) {
                    Text(context.getString(R.string.ok))
                }
            }
        )
    }

    // Bilibili 搜索结果列表
    if (showBilibiliSheet) {
        var selectedTag by remember { mutableStateOf("白噪音") }

        ModalBottomSheet(
            onDismissRequest = { showBilibiliSheet = false; isEditMode = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(8f / 9f)
            ) {
                LaunchedEffect(selectedTag) {
                    viewModel.searchBilibili(selectedTag)
                }

                LaunchedEffect(bilibiliRooms) {
                    viewModel.syncPinnedRoomInfos(bilibiliRooms)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tags = remember {
                        listOf(
                            Pair(R.string.tag_white_noise, "白噪音"),
                            Pair(R.string.tag_study_room, "自习室")
                        )
                    }
                    for (tag in tags) {
                        val (labelRes, keyword) = tag
                        FilterChip(
                            selected = selectedTag == keyword,
                            onClick = { selectedTag = keyword },
                            label = { Text(stringResource(labelRes)) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEditMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        IconButton(onClick = { viewModel.refreshBilibiliSearch() }) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = stringResource(R.string.bilibili_room_list_hint),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                val pinnedRooms = pinnedRoomInfos
                val otherRooms = bilibiliRooms.filter { it.roomId !in pinnedRoomIds }

                val roomLiveStatus = remember { mutableStateMapOf<String, Boolean>() }

                LaunchedEffect(pinnedRooms) {
                    roomLiveStatus.clear()
                    for (room in pinnedRooms) {
                        val info = withContext(Dispatchers.IO) { BilibiliApi.getRoomInfo(room.roomId) }
                        roomLiveStatus[room.roomId] = info?.isLive == true
                    }
                }

                if (bilibiliRooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(R.string.no_live), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 白噪音置顶
                        if (pinnedRooms.isNotEmpty()) {
                            item {
                                SectionHeader(title = stringResource(R.string.recommended))
                            }
                            items(pinnedRooms, key = { it.roomId }) { room ->
                                RoomCard(
                                    room = room,
                                    isPlaying = isPlaying,
                                    playingRoomId = playingRoomId,
                                    isEditMode = isEditMode,
                                    isPinned = true,
                                    isLive = roomLiveStatus[room.roomId] ?: false,
                                    onPinToggle = { viewModel.togglePinRoom(room.roomId, room) },
                                    onPlay = {
                                        showBilibiliSheet = false
                                        isEditMode = false
                                        viewModel.setPlayingRoomInfo(room)
                                        viewModel.playBilibiliRoom(room.roomId, room)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        // 搜索结果
                        if (otherRooms.isNotEmpty()) {
                            item {
                                SectionHeader(title = stringResource(R.string.search_result, selectedTag))
                            }
                            items(otherRooms, key = { it.roomId }) { room ->
                                RoomCard(
                                    room = room,
                                    isPlaying = isPlaying,
                                    playingRoomId = playingRoomId,
                                    isEditMode = isEditMode,
                                    isPinned = room.roomId in pinnedRoomIds,
                                    onPinToggle = { viewModel.togglePinRoom(room.roomId, room) },
                                    onPlay = {
                                        showBilibiliSheet = false
                                        isEditMode = false
                                        viewModel.setPlayingRoomInfo(room)
                                        viewModel.playBilibiliRoom(room.roomId, room)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RoomCard(
    room: BilibiliApi.LiveRoom,
    isPlaying: Boolean,
    playingRoomId: String?,
    isEditMode: Boolean = false,
    isPinned: Boolean = false,
    isLive: Boolean = true,
    onPinToggle: () -> Unit = {},
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (room.roomId == playingRoomId && isPlaying)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface,
        onClick = {
            if (isLive) {
                onPlay()
            } else {
                Toast.makeText(context, R.string.room_not_live, Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        !isLive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        room.roomId == playingRoomId && isPlaying -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "${room.userName} · ${room.online} 人",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!isLive)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    else if (room.roomId == playingRoomId && isPlaying)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isEditMode) {
                IconButton(onClick = onPinToggle) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (room.roomId == playingRoomId && isPlaying) {
                EqualizerBars(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EqualizerBars(color: Color, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val barHeights = listOf(
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse), label = "bar1"),
        transition.animateFloat(1f, 0.4f, infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse), label = "bar2"),
        transition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "bar3"),
    )
    Canvas(modifier = modifier) {
        val barWidth = size.width / 5
        val gap = barWidth / 2
        barHeights.forEachIndexed { i, bar ->
            val barHeight = size.height * bar.value
            drawRect(
                color = color,
                size = Size(barWidth, barHeight),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = gap + i * (barWidth + gap),
                    y = size.height - barHeight
                )
            )
        }
    }
}


