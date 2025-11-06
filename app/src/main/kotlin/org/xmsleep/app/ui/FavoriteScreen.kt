package org.xmsleep.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.xmsleep.app.*
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager

/**
 * 收藏页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = false,
    columnsCount: Int = 2,
    pinnedSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    onBack: () -> Unit,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { AudioManager.getInstance() }
    val colorScheme = MaterialTheme.colorScheme
    
    // 各声音的播放状态
    val playingStates = remember { mutableStateMapOf<AudioManager.Sound, Boolean>() }
    
    // 6个声音模块的数据（使用字符串资源以支持语言切换）
    val configuration = LocalConfiguration.current
    val soundItems = remember(colorScheme, configuration.locales) {
        listOf(
            SoundItem(
                AudioManager.Sound.RAIN,
                context.getString(R.string.sound_rain),
                R.raw.rain_animation_optimized,
                colorScheme.primary
            ),
            SoundItem(
                AudioManager.Sound.CAMPFIRE,
                context.getString(R.string.sound_campfire),
                R.raw.gouhuo_animation,
                colorScheme.error
            ),
            SoundItem(
                AudioManager.Sound.THUNDER,
                context.getString(R.string.sound_thunder),
                R.raw.dalei_animation,
                colorScheme.secondary
            ),
            SoundItem(
                AudioManager.Sound.CAT_PURRING,
                context.getString(R.string.sound_cat_purring),
                R.raw.cat,
                colorScheme.tertiary
            ),
            SoundItem(
                AudioManager.Sound.BIRD_CHIRPING,
                context.getString(R.string.sound_bird_chirping),
                R.raw.bird,
                colorScheme.primaryContainer
            ),
            SoundItem(
                AudioManager.Sound.NIGHT_INSECTS,
                context.getString(R.string.sound_night_insects),
                R.raw.xishuai,
                colorScheme.secondaryContainer
            )
        )
    }
    
    // 过滤出收藏的声音
    val favoriteItems = remember(favoriteSounds.value) {
        soundItems.filter { favoriteSounds.value.contains(it.sound) }
    }
    
    // 滚动状态
    val scrollState = rememberLazyGridState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.tab_favorite),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // 返回导航按钮
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-4).dp)
                    ) {
                        Box(Modifier.size(24.dp)) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = context.getString(R.string.go_back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                // TopAppBar 使用系统栏和显示区域切口，让标题向上移动
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // TopAppBar 使用的 insets
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .padding(paddingValues)
        ) {
            // 内容区域
            if (favoriteItems.isEmpty()) {
                // 没有收藏时显示占位符
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EmptyStateAnimation(size = 240.dp)
                        Text(
                            context.getString(R.string.no_favorites),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            context.getString(R.string.favorites_will_show_here),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // 有收藏时显示卡片列表
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favoriteItems) { item ->
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        
                        SoundCard(
                            item = item,
                            isPlaying = playingStates[item.sound] ?: false,
                            hideAnimation = hideAnimation,
                            columnsCount = columnsCount,
                            isPinned = pinnedSounds.value.contains(item.sound),
                            isFavorite = favoriteSounds.value.contains(item.sound),
                            onToggle = { sound ->
                                val wasPlaying = audioManager.isPlayingSound(sound)
                                if (wasPlaying) {
                                    audioManager.pauseSound(sound)
                                    playingStates[sound] = false
                                } else {
                                    audioManager.playSound(context, sound)
                                    playingStates[sound] = true
                                }
                            },
                            onVolumeClick = { showVolumeDialog = true },
                            onTitleClick = { },
                            onPinnedChange = { isPinned ->
                                onPinnedChange(item.sound, isPinned)
                                // 如果声音正在播放，立即同步播放状态
                                playingStates[item.sound] = audioManager.isPlayingSound(item.sound)
                            },
                            onFavoriteChange = { isFavorite ->
                                onFavoriteChange(item.sound, isFavorite)
                            },
                            modifier = Modifier
                        )
                        
                        // 音量调节弹窗
                        if (showVolumeDialog) {
                            var currentVolume by remember { mutableStateOf(audioManager.getVolume(item.sound)) }
                            VolumeDialog(
                                sound = item.sound,
                                currentVolume = currentVolume,
                                onDismiss = { showVolumeDialog = false },
                                onVolumeChange = { newVolume ->
                                    currentVolume = newVolume
                                    audioManager.setVolume(item.sound, newVolume)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
