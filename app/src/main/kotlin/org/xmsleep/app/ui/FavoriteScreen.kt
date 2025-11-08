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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.*
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.AudioCacheManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.i18n.LanguageManager
import android.widget.Toast

/**
 * 收藏页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = false,
    columnsCount: Int = 3, // 默认3列
    pinnedSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: androidx.compose.runtime.MutableState<MutableSet<AudioManager.Sound>>,
    onBack: () -> Unit,
    onPinnedChange: (AudioManager.Sound, Boolean) -> Unit,
    onFavoriteChange: (AudioManager.Sound, Boolean) -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { AudioManager.getInstance() }
    val resourceManager = remember { AudioResourceManager.getInstance(context) }
    val cacheManager = remember { AudioCacheManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme
    
    // 各声音的播放状态
    val playingStates = remember { mutableStateMapOf<AudioManager.Sound, Boolean>() }
    
    // 远程音频相关状态
    var remoteSounds by remember { mutableStateOf<List<org.xmsleep.app.audio.model.SoundMetadata>>(emptyList()) }
    var remoteFavorites by remember { 
        mutableStateOf(PreferencesManager.getRemoteFavorites(context).toMutableSet()) 
    }
    var remotePinned by remember { 
        mutableStateOf(PreferencesManager.getRemotePinned(context).toMutableSet()) 
    }
    var downloadingSounds by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var playingRemoteSounds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // 获取当前语言
    val currentLanguage = LanguageManager.getCurrentLanguage(context)
    val isEnglish = currentLanguage == LanguageManager.Language.ENGLISH
    val isTraditionalChinese = currentLanguage == LanguageManager.Language.TRADITIONAL_CHINESE
    
    // 获取分类显示名称的辅助函数
    fun getCategoryDisplayName(categoryId: String): String {
        // 这里可以根据需要实现分类名称的国际化
        return categoryId
    }
    
    // 获取音频显示名称的辅助函数
    fun getSoundDisplayName(sound: org.xmsleep.app.audio.model.SoundMetadata): String {
        return when {
            isTraditionalChinese && !sound.nameZhTW.isNullOrEmpty() -> sound.nameZhTW
            isEnglish && !sound.nameEn.isNullOrEmpty() -> sound.nameEn
            else -> sound.name
        }
    }
    
    // 14个声音模块的数据（使用字符串资源以支持语言切换）
    val configuration = LocalConfiguration.current
    val soundItems = remember(colorScheme, configuration.locales) {
        listOf(
            SoundItem(
                AudioManager.Sound.UMBRELLA_RAIN,
                context.getString(R.string.sound_umbrella_rain),
                R.drawable.umbrella_rain,
                colorScheme.primary
            ),
            SoundItem(
                AudioManager.Sound.ROWING,
                context.getString(R.string.sound_rowing),
                R.drawable.rowing,
                colorScheme.error
            ),
            SoundItem(
                AudioManager.Sound.OFFICE,
                context.getString(R.string.sound_office),
                R.drawable.office,
                colorScheme.secondary
            ),
            SoundItem(
                AudioManager.Sound.LIBRARY,
                context.getString(R.string.sound_library),
                R.drawable.library,
                colorScheme.tertiary
            ),
            SoundItem(
                AudioManager.Sound.HEAVY_RAIN,
                context.getString(R.string.sound_heavy_rain),
                R.drawable.heavy_rain,
                colorScheme.primaryContainer
            ),
            SoundItem(
                AudioManager.Sound.TYPEWRITER,
                context.getString(R.string.sound_typewriter),
                R.drawable.typewriter,
                colorScheme.secondaryContainer
            ),
            SoundItem(
                AudioManager.Sound.THUNDER_NEW,
                context.getString(R.string.sound_thunder_new),
                R.drawable.thunder_new,
                colorScheme.tertiaryContainer
            ),
            SoundItem(
                AudioManager.Sound.CLOCK,
                context.getString(R.string.sound_clock),
                R.drawable.clock,
                colorScheme.errorContainer
            ),
            SoundItem(
                AudioManager.Sound.FOREST_BIRDS,
                context.getString(R.string.sound_forest_birds),
                R.drawable.forest_birds,
                colorScheme.primary
            ),
            SoundItem(
                AudioManager.Sound.DRIFTING,
                context.getString(R.string.sound_drifting),
                R.drawable.drifting,
                colorScheme.error
            ),
            SoundItem(
                AudioManager.Sound.CAMPFIRE_NEW,
                context.getString(R.string.sound_campfire_new),
                R.drawable.campfire_new,
                colorScheme.secondary
            ),
            SoundItem(
                AudioManager.Sound.WIND,
                context.getString(R.string.sound_wind),
                R.drawable.wind,
                colorScheme.tertiary
            ),
            SoundItem(
                AudioManager.Sound.KEYBOARD,
                context.getString(R.string.sound_keyboard),
                R.drawable.keyboard,
                colorScheme.primaryContainer
            ),
            SoundItem(
                AudioManager.Sound.SNOW_WALKING,
                context.getString(R.string.sound_snow_walking),
                R.drawable.snow_walking,
                colorScheme.secondaryContainer
            )
        )
    }
    
    // 过滤出收藏的本地声音
    val favoriteItems = remember(favoriteSounds.value) {
        soundItems.filter { favoriteSounds.value.contains(it.sound) }
    }
    
    // 过滤出收藏的远程声音
    val favoriteRemoteSounds = remember(remoteFavorites, remoteSounds) {
        remoteSounds.filter { remoteFavorites.contains(it.id) }
    }
    
    // 加载远程音频列表
    LaunchedEffect(Unit) {
        try {
            val sounds = resourceManager.getRemoteSounds()
            remoteSounds = sounds
        } catch (e: Exception) {
            android.util.Log.e("FavoriteScreen", "加载远程音频失败: ${e.message}")
        }
    }
    
    // 监听远程音频播放状态
    LaunchedEffect(remoteSounds, remoteFavorites) {
        while (true) {
            delay(500)
            val currentlyPlaying = remoteSounds.filter { sound ->
                remoteFavorites.contains(sound.id) && audioManager.isPlayingRemoteSound(sound.id)
            }.map { it.id }.toSet()
            playingRemoteSounds = currentlyPlaying
        }
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
            val allFavoritesEmpty = favoriteItems.isEmpty() && favoriteRemoteSounds.isEmpty()
            if (allFavoritesEmpty) {
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
                    // 本地音频收藏
                    items(favoriteItems) { item ->
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        
                        // 根据列数使用不同的无动画卡片组件，与远程卡片保持一致
                        if (columnsCount == 3) {
                            SimpleSoundCard3Columns(
                                item = item,
                                isPlaying = playingStates[item.sound] ?: false,
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
                        } else {
                            SimpleSoundCard2Columns(
                                item = item,
                                isPlaying = playingStates[item.sound] ?: false,
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
                        }
                        
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
                    
                    // 远程音频收藏
                    items(favoriteRemoteSounds) { sound ->
                        var showVolumeDialog by remember { mutableStateOf(false) }
                        var showSoundMenuDialog by remember { mutableStateOf(false) }
                        var isCached by remember { mutableStateOf(cacheManager.getCachedFile(sound.id) != null) }
                        val downloadProgress = downloadingSounds[sound.id]
                        val isPlaying = playingRemoteSounds.contains(sound.id)
                        
                        // 监听下载完成，更新缓存状态
                        LaunchedEffect(downloadProgress, sound.id) {
                            if (downloadProgress == null) {
                                isCached = cacheManager.getCachedFile(sound.id) != null
                            }
                            if (downloadProgress != null && downloadProgress >= 1.0f) {
                                isCached = cacheManager.getCachedFile(sound.id) != null
                            }
                        }
                        
                        // 使用 MainActivity 中的 RemoteSoundCard
                        org.xmsleep.app.RemoteSoundCard(
                            sound = sound,
                            displayName = getSoundDisplayName(sound),
                            isPlaying = isPlaying,
                            downloadProgress = downloadProgress,
                            columnsCount = columnsCount,
                            isPinned = remotePinned.contains(sound.id),
                            isFavorite = remoteFavorites.contains(sound.id),
                            onPinnedChange = { isPinned ->
                                val newSet = remotePinned.toMutableSet()
                                if (isPinned) {
                                    if (newSet.size >= 3) {
                                        Toast.makeText(context, context.getString(R.string.max_3_sounds_limit), Toast.LENGTH_SHORT).show()
                                    } else {
                                        newSet.add(sound.id)
                                    }
                                } else {
                                    newSet.remove(sound.id)
                                }
                                remotePinned = newSet
                                PreferencesManager.saveRemotePinned(context, newSet)
                            },
                            onFavoriteChange = { isFavorite ->
                                val newSet = remoteFavorites.toMutableSet()
                                if (isFavorite) {
                                    newSet.add(sound.id)
                                } else {
                                    newSet.remove(sound.id)
                                }
                                remoteFavorites = newSet
                                PreferencesManager.saveRemoteFavorites(context, newSet)
                            },
                            onCardClick = {
                                scope.launch {
                                    try {
                                        val cachedFile = cacheManager.getCachedFile(sound.id)
                                        if (cachedFile == null && sound.remoteUrl != null) {
                                            // 开始下载
                                            val downloadFlow = cacheManager.downloadAudioWithProgress(
                                                sound.remoteUrl,
                                                sound.id
                                            )
                                            downloadFlow.collect { progress ->
                                                when (progress) {
                                                    is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                        val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                        downloadingSounds = downloadingSounds + (sound.id to percent)
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                        downloadingSounds = downloadingSounds - sound.id
                                                        isCached = true
                                                        // 下载完成后自动播放
                                                        scope.launch {
                                                            val uri = resourceManager.getSoundUri(sound)
                                                            if (uri != null) {
                                                                audioManager.playRemoteSound(context, sound, uri)
                                                                // 更新播放状态
                                                                playingRemoteSounds = playingRemoteSounds + sound.id
                                                            }
                                                        }
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                        downloadingSounds = downloadingSounds - sound.id
                                                        Toast.makeText(context, "下载失败: ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        } else {
                                            // 已缓存，直接播放
                                            scope.launch {
                                                val uri = resourceManager.getSoundUri(sound)
                                                if (uri != null) {
                                                    if (audioManager.isPlayingRemoteSound(sound.id)) {
                                                        audioManager.pauseRemoteSound(sound.id)
                                                        // 更新播放状态
                                                        playingRemoteSounds = playingRemoteSounds - sound.id
                                                    } else {
                                                        audioManager.playRemoteSound(context, sound, uri)
                                                        // 更新播放状态
                                                        playingRemoteSounds = playingRemoteSounds + sound.id
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onVolumeClick = { showVolumeDialog = true }
                        )
                        
                        // 音量调节弹窗
                        if (showVolumeDialog) {
                            var currentVolume by remember { mutableStateOf(audioManager.getRemoteVolume(sound.id)) }
                            AlertDialog(
                                onDismissRequest = { showVolumeDialog = false },
                                title = { Text(getSoundDisplayName(sound)) },
                                text = {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            context.getString(R.string.adjust_volume),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Slider(
                                            value = currentVolume,
                                            onValueChange = { 
                                                currentVolume = it
                                                audioManager.setRemoteVolume(sound.id, it)
                                            },
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
                                                text = "${(currentVolume * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showVolumeDialog = false }) {
                                        Text(context.getString(R.string.cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
