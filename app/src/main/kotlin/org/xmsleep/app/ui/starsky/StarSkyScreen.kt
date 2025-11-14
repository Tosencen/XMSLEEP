package org.xmsleep.app.ui.starsky

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.ui.components.pagerTabIndicatorOffset

/**
 * 星空页面 - 远程音频浏览和管理
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarSkyScreen(
    modifier: Modifier = Modifier,
    onScrollDetected: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 获取当前语言（不使用remember，确保语言切换后能更新）
    val currentLanguage = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
    val isEnglish = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.ENGLISH
    val isTraditionalChinese = currentLanguage == org.xmsleep.app.i18n.LanguageManager.Language.TRADITIONAL_CHINESE
    
    // 音频资源管理器
    val resourceManager = remember { 
        org.xmsleep.app.audio.AudioResourceManager.getInstance(context) 
    }
    val audioManager = remember { 
        org.xmsleep.app.audio.AudioManager.getInstance() 
    }
    val cacheManager = remember { 
        org.xmsleep.app.audio.AudioCacheManager.getInstance(context) 
    }
    
    // 状态 - 初始化时从缓存加载，避免切换tab时重复加载
    val initialCachedManifest = remember { resourceManager.getCachedManifest() }
    var remoteSounds by remember { 
        mutableStateOf(initialCachedManifest?.sounds ?: emptyList())
    }
    var remoteCategories by remember { 
        mutableStateOf(initialCachedManifest?.categories ?: emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var downloadingSounds by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var downloadingButNoProgress by remember { mutableStateOf<Set<String>>(emptySet()) } // 正在下载但还没有收到进度
    var playingSounds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var columnsCount by remember { 
        mutableIntStateOf(org.xmsleep.app.preferences.PreferencesManager.getStarSkyColumnsCount(context)) 
    }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var selectedSoundForVolume by remember { mutableStateOf<org.xmsleep.app.audio.model.SoundMetadata?>(null) }
    var volume by remember { mutableStateOf(1f) }
    var remoteFavorites by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getRemoteFavorites(context).toMutableSet()) 
    }
    var remotePinned by remember { 
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getRemotePinned(context).toMutableSet()) 
    }
    
    
    // 监听播放状态变化
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // 每500ms检查一次播放状态
            val currentlyPlaying = remoteSounds.filter { sound ->
                audioManager.isPlayingRemoteSound(sound.id)
            }.map { it.id }.toSet()
            playingSounds = currentlyPlaying
        }
    }
    
    // 加载音频清单 - 优化：先显示缓存，后台刷新，避免重复加载
    LaunchedEffect(Unit) {
        // 如果已经有数据（从缓存初始化），不重新加载（避免切换tab时重复加载）
        if (remoteSounds.isNotEmpty() && remoteCategories.isNotEmpty()) {
            android.util.Log.d("StarSkyScreen", "已有缓存数据，跳过加载，直接后台刷新")
            // 有缓存数据，延迟刷新（避免频繁请求），只在后台静默更新
            delay(5000) // 延迟5秒后刷新，避免频繁请求
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "后台刷新清单成功，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 后台刷新失败不影响显示
                android.util.Log.e("StarSkyScreen", "后台刷新音频清单失败: ${e.message}")
            }
            return@LaunchedEffect
        }
        
        // 没有缓存数据，需要加载
        errorMessage = null
        
        // 第一步：先尝试从缓存加载（同步，快速），立即显示（不显示加载状态）
        val cachedManifest = resourceManager.getCachedManifest()
        if (cachedManifest != null) {
            remoteSounds = cachedManifest.sounds
            remoteCategories = cachedManifest.categories
            android.util.Log.d("StarSkyScreen", "从缓存加载清单，分类数量: ${cachedManifest.categories.size}")
            isLoading = false // 有缓存数据，不显示加载状态
        } else {
            // 完全没有缓存，显示加载状态
            isLoading = true
        }
        
        // 第二步：后台刷新网络数据（不阻塞UI）
        if (remoteSounds.isEmpty()) {
            // 没有数据，立即刷新
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "成功刷新清单，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 刷新失败不影响显示，只记录错误
                if (remoteSounds.isEmpty()) {
                    // 只有在完全没有数据时才显示错误
                    errorMessage = e.message
                }
                android.util.Log.e("StarSkyScreen", "刷新音频清单失败: ${e.message}")
            } finally {
                isLoading = false
            }
        } else {
            // 有数据，延迟刷新（避免频繁请求），只在后台静默更新
            delay(5000) // 延迟5秒后刷新，避免频繁请求
            try {
                val refreshedManifest = resourceManager.refreshRemoteManifest().getOrNull()
                if (refreshedManifest != null) {
                    remoteSounds = refreshedManifest.sounds
                    remoteCategories = refreshedManifest.categories
                    android.util.Log.d("StarSkyScreen", "后台刷新清单成功，分类数量: ${refreshedManifest.categories.size}")
                }
            } catch (e: Exception) {
                // 后台刷新失败不影响显示
                android.util.Log.e("StarSkyScreen", "后台刷新音频清单失败: ${e.message}")
            }
        }
    }
    
    // 获取分类显示名称的辅助函数
    fun getCategoryDisplayName(categoryId: String): String {
        val category = remoteCategories.find { it.id == categoryId }
        if (category != null) {
            return when {
                isEnglish && category.nameEn != null -> category.nameEn
                isTraditionalChinese && category.nameZhTW != null -> category.nameZhTW
                isTraditionalChinese -> category.name // 如果没有繁体中文，使用简体中文
                else -> category.name
            }
        }
        
        // 如果找不到分类，使用后备映射（确保不会显示英文）
        val fallbackMap = mapOf(
            "nature" to if (isTraditionalChinese) "自然" else if (isEnglish) "Nature" else "自然",
            "rain" to if (isTraditionalChinese) "雨聲" else if (isEnglish) "Rain" else "雨声",
            "urban" to if (isTraditionalChinese) "城市" else if (isEnglish) "Urban" else "城市",
            "places" to if (isTraditionalChinese) "場所" else if (isEnglish) "Places" else "场所",
            "transport" to if (isTraditionalChinese) "交通" else if (isEnglish) "Transport" else "交通",
            "things" to if (isTraditionalChinese) "物品" else if (isEnglish) "Things" else "物品",
            "noise" to if (isTraditionalChinese) "噪音" else if (isEnglish) "Noise" else "噪音",
            "animals" to if (isTraditionalChinese) "動物" else if (isEnglish) "Animals" else "动物",
        )
        
        return fallbackMap[categoryId] ?: categoryId
    }
    
    // 获取音频显示名称的辅助函数
    fun getSoundDisplayName(sound: org.xmsleep.app.audio.model.SoundMetadata): String {
        return when {
            isEnglish && sound.nameEn != null -> sound.nameEn
            isTraditionalChinese && sound.nameZhTW != null -> sound.nameZhTW
            isTraditionalChinese -> sound.name // 如果没有繁体中文，使用简体中文
            else -> sound.name
        }
    }
    
    // 按分类分组
    val soundsByCategory = remember(remoteSounds) {
        remoteSounds.groupBy { it.category }
    }
    
    // 获取分类列表（使用分类ID，按照JSON中的order字段排序）
    val categoryIds = remember(remoteSounds, remoteCategories) {
        val categoryIdsFromSounds = remoteSounds.map { it.category }.distinct().toSet()
        // 优先使用 remoteCategories 中的 order 字段排序
        if (remoteCategories.isNotEmpty()) {
            val sorted = remoteCategories
                .filter { it.id in categoryIdsFromSounds }
                .sortedBy { it.order }
                .map { it.id }
            android.util.Log.d("StarSkyScreen", "分类排序: ${sorted.joinToString { it }}")
            android.util.Log.d("StarSkyScreen", "分类详情: ${remoteCategories.map { "${it.id}: order=${it.order}" }.joinToString()}")
            sorted
        } else {
            // 如果没有分类信息，使用字符串排序作为后备
            android.util.Log.w("StarSkyScreen", "remoteCategories 为空，使用字符串排序")
            categoryIdsFromSounds.sorted()
        }
    }
    
    // 保存每个分类的滚动状态，使用 remember 确保 tab 切换时保留
    // 使用 Map 来存储每个分类的滚动状态，key 是分类 ID（null 表示"全部"）
    val listStates = remember { mutableMapOf<String?, LazyListState>() }
    val gridStates = remember { mutableMapOf<String?, LazyGridState>() }
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // 标题栏（包含标题和布局切换按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.star_sky),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // 布局切换按钮
            IconButton(
                onClick = { 
                    val newColumnsCount = if (columnsCount == 2) 3 else 2
                    columnsCount = newColumnsCount
                    org.xmsleep.app.preferences.PreferencesManager.saveStarSkyColumnsCount(context, newColumnsCount)
                }
            ) {
                Icon(
                    imageVector = if (columnsCount == 2) Icons.Default.GridView else Icons.Outlined.ViewAgenda,
                    contentDescription = if (columnsCount == 2) 
                        context.getString(R.string.switch_to_3_columns) 
                    else 
                        context.getString(R.string.switch_to_2_columns),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 加载状态
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // 错误状态
        else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
        horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = context.getString(R.string.load_failed, errorMessage ?: ""),
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val manifest = resourceManager.refreshRemoteManifest().getOrNull()
                                    if (manifest != null) {
                                        remoteSounds = manifest.sounds
                                        remoteCategories = manifest.categories
                                    } else {
                                        val sounds = resourceManager.getRemoteSounds()
                                        remoteSounds = sounds
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text(context.getString(R.string.retry))
                    }
                }
            }
        }
        // 音频列表
        else if (remoteSounds.isNotEmpty()) {
            // 构建所有分类列表（包括"全部"）
            val allCategories = remember(categoryIds) { listOf(null) + categoryIds }
            
            // 使用PagerState来管理页面状态，实现左右滑动
            val initialPage = remember(allCategories) { 
                allCategories.indexOfFirst { it == selectedCategory }.coerceAtLeast(0) 
            }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { allCategories.size }
            )
            
            // 同步PagerState和selectedCategory
            LaunchedEffect(pagerState.currentPage) {
                val newCategory = allCategories.getOrNull(pagerState.currentPage)
                if (newCategory != selectedCategory) {
                    selectedCategory = newCategory
                }
            }
            
            // 同步selectedCategory和PagerState
            LaunchedEffect(selectedCategory) {
                val targetIndex = allCategories.indexOfFirst { it == selectedCategory }
                if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
                    pagerState.animateScrollToPage(targetIndex)
                }
            }
            
            // 分类筛选 - 使用Tab Row样式（传统标签页样式）
            if (categoryIds.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    allCategories.forEachIndexed { index, categoryId ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                scope.launch { 
                                    pagerState.animateScrollToPage(index) 
                                }
                            },
                            text = {
                                Text(
                                    text = if (categoryId == null) 
                                        context.getString(R.string.all_categories) 
                                    else 
                                        getCategoryDisplayName(categoryId),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
            
            // 音频列表 - 使用HorizontalPager实现左右滑动
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                pageSpacing = 0.dp
            ) { pageIndex ->
                val category = allCategories[pageIndex]
                if (category == null) {
                    // "全部"类别下，按分类分组显示
                    val soundsByCategory = remoteSounds.groupBy { it.category }
                    val sortedCategories = categoryIds.filter { soundsByCategory.containsKey(it) }
                    
                    // 使用 remember 配合分类 ID 作为 key，确保 tab 切换时保留滚动状态
                    // 直接从 Map 中获取或创建状态，由于 listStates 在 remember 中保存，状态会在 tab 切换时保留
                    val lazyListState = listStates.getOrPut(null) { rememberLazyListState() }
                    
                    // 监听滚动状态，触发浮动按钮收缩
                    LaunchedEffect(lazyListState.isScrollInProgress) {
                        if (lazyListState.isScrollInProgress) {
                            onScrollDetected()
                        }
                    }
                    
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                    sortedCategories.forEach { categoryId ->
                        val categorySounds = soundsByCategory[categoryId] ?: emptyList()
                        if (categorySounds.isNotEmpty()) {
                            // 分类标题
                            item(key = "category_$categoryId") {
                                Text(
                                    text = getCategoryDisplayName(categoryId),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            // 该分类下的卡片网格（使用非Lazy Grid避免嵌套滚动）
                            item(key = "grid_$categoryId") {
                                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                                val horizontalPadding = 32.dp
                                val spacing = 12.dp * (columnsCount - 1)
                                val cardWidth = (screenWidth - horizontalPadding - spacing) / columnsCount
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    categorySounds.chunked(columnsCount).forEach { rowSounds ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowSounds.forEach { sound ->
                                                Box(
                                                    modifier = Modifier.width(cardWidth)
                                                ) {
                                                    RemoteSoundCard(
                        sound = sound,
                        displayName = getSoundDisplayName(sound),
                        isPlaying = playingSounds.contains(sound.id),
                        downloadProgress = downloadingSounds[sound.id],
                        isDownloadingButNoProgress = downloadingButNoProgress.contains(sound.id),
                        columnsCount = columnsCount,
                        isPinned = remotePinned.contains(sound.id),
                        isFavorite = remoteFavorites.contains(sound.id),
                        onPinnedChange = { isPinned ->
                            val newSet = remotePinned.toMutableSet()
                            if (isPinned) {
                                // 检查音频是否已下载
                                val cachedFile = cacheManager.getCachedFile(sound.id)
                                if (cachedFile == null) {
                                    // 未下载，不允许置顶
                                    Toast.makeText(context, context.getString(R.string.must_download_before_pin), Toast.LENGTH_SHORT).show()
                                } else {
                                    // 检查是否已达到最大数量（3个）
                                    if (newSet.size >= 3) {
                                        Toast.makeText(context, context.getString(R.string.max_3_sounds_limit), Toast.LENGTH_SHORT).show()
                                    } else {
                                        newSet.add(sound.id)
                                        remotePinned = newSet
                                        org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                    }
                                }
                            } else {
                                newSet.remove(sound.id)
                                remotePinned = newSet
                                org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                            }
                        },
                        onFavoriteChange = { isFavorite ->
                            val newSet = remoteFavorites.toMutableSet()
                            if (isFavorite) {
                                newSet.add(sound.id)
                            } else {
                                newSet.remove(sound.id)
                            }
                            remoteFavorites = newSet
                            org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(context, newSet)
                        },
                        onCardClick = {
                            scope.launch {
                                try {
                                    // 首先检查是否正在播放，如果正在播放则停止播放
                                    val currentlyPlaying = audioManager.isPlayingRemoteSound(sound.id)
                                    if (currentlyPlaying) {
                                        audioManager.pauseRemoteSound(sound.id)
                                        return@launch
                                    }
                                    
                                    // 如果未播放，检查是否需要下载
                                    val cachedFile = cacheManager.getCachedFile(sound.id)
                                    if (cachedFile == null && sound.remoteUrl != null) {
                                        // 开始下载，添加到"正在下载但还没有进度"集合
                                        downloadingButNoProgress = downloadingButNoProgress + sound.id
                                        // 开始下载
                                        val downloadFlow = cacheManager.downloadAudioWithProgress(
                                            sound.remoteUrl,
                                            sound.id
                                        )
                                        downloadFlow.collect { progress ->
                                            when (progress) {
                                                is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                    val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                    android.util.Log.d("StarSkyScreen", "下载进度: ${sound.id} = $percent")
                                                    // 收到第一个进度更新，从"正在下载但还没有进度"集合中移除
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        put(sound.id, percent)
                                                    }
                                                }
                                                is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                    android.util.Log.d("StarSkyScreen", "下载完成: ${sound.id}")
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        remove(sound.id)
                                                    }
                                                    // 下载完成后，增加缓冲时间再播放
                                                    val uri = resourceManager.getSoundUri(sound)
                                                    if (uri != null) {
                                                        // 延迟200ms确保文件系统写入完成
                                                        delay(200)
                                                        audioManager.playRemoteSound(context, sound, uri)
                                                        playingSounds = playingSounds + sound.id
                                                    } else {
                                                        android.util.Log.e("StarSkyScreen", "下载完成后无法获取URI: ${sound.id}")
                                                        Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                                    }
                                                    return@collect
                                                }
                                                is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                    android.util.Log.e("StarSkyScreen", "下载失败: ${sound.id} - ${progress.exception.message}")
                                                    downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                    downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                        remove(sound.id)
                                                    }
                                                    Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                                                    return@collect
                                                }
                                            }
                                        }
                                    } else {
                                        // 已缓存或直接播放
                                        val uri = resourceManager.getSoundUri(sound)
                                        if (uri != null) {
                                            audioManager.playRemoteSound(context, sound, uri)
                                            playingSounds = playingSounds + sound.id
                                        } else {
                                            android.util.Log.e("StarSkyScreen", "无法获取URI: ${sound.id}")
                                            Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("StarSkyScreen", "播放失败: ${e.message}")
                                    Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                                            },
                                            onVolumeClick = {
                                                selectedSoundForVolume = sound
                                                volume = audioManager.getRemoteVolume(sound.id)
                                                showVolumeDialog = true
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
                }
                } else {
                    // 其他分类，直接显示
                    val filteredSounds = remoteSounds.filter { it.category == category }
                    
                    // 使用 remember 配合分类 ID 作为 key，确保 tab 切换时保留滚动状态
                    // 直接从 Map 中获取或创建状态，由于 gridStates 在 remember 中保存，状态会在 tab 切换时保留
                    val lazyGridState = gridStates.getOrPut(category) { rememberLazyGridState() }
                    
                    // 监听滚动状态，触发浮动按钮收缩
                    LaunchedEffect(lazyGridState.isScrollInProgress) {
                        if (lazyGridState.isScrollInProgress) {
                            onScrollDetected()
                        }
                    }
                    
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Fixed(columnsCount),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                    items(filteredSounds.size) { index ->
                        val sound = filteredSounds[index]
                        RemoteSoundCard(
                            sound = sound,
                            displayName = getSoundDisplayName(sound),
                            isPlaying = playingSounds.contains(sound.id),
                            downloadProgress = downloadingSounds[sound.id],
                            isDownloadingButNoProgress = downloadingButNoProgress.contains(sound.id),
                            columnsCount = columnsCount,
                            isPinned = remotePinned.contains(sound.id),
                            isFavorite = remoteFavorites.contains(sound.id),
                            onPinnedChange = { isPinned ->
                                val newSet = remotePinned.toMutableSet()
                                if (isPinned) {
                                    // 检查音频是否已下载
                                    val cachedFile = cacheManager.getCachedFile(sound.id)
                                    if (cachedFile == null) {
                                        // 未下载，不允许置顶
                                        Toast.makeText(context, context.getString(R.string.must_download_before_pin), Toast.LENGTH_SHORT).show()
                                    } else {
                                        // 检查是否已达到最大数量（3个）
                                        if (newSet.size >= 3) {
                                            Toast.makeText(context, context.getString(R.string.max_3_sounds_limit), Toast.LENGTH_SHORT).show()
                                        } else {
                                            newSet.add(sound.id)
                                            remotePinned = newSet
                                            org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                        }
                                    }
                                } else {
                                    newSet.remove(sound.id)
                                    remotePinned = newSet
                                    org.xmsleep.app.preferences.PreferencesManager.saveRemotePinned(context, newSet)
                                }
                            },
                            onFavoriteChange = { isFavorite ->
                                val newSet = remoteFavorites.toMutableSet()
                                if (isFavorite) {
                                    newSet.add(sound.id)
                                } else {
                                    newSet.remove(sound.id)
                                }
                                remoteFavorites = newSet
                                org.xmsleep.app.preferences.PreferencesManager.saveRemoteFavorites(context, newSet)
                            },
                            onCardClick = {
                                scope.launch {
                                    try {
                                        // 首先检查是否正在播放，如果正在播放则停止播放
                                        val currentlyPlaying = audioManager.isPlayingRemoteSound(sound.id)
                                        if (currentlyPlaying) {
                                            audioManager.pauseRemoteSound(sound.id)
                                            return@launch
                                        }
                                        
                                        // 如果未播放，检查是否需要下载
                                        val cachedFile = cacheManager.getCachedFile(sound.id)
                                        if (cachedFile == null && sound.remoteUrl != null) {
                                            // 开始下载，添加到"正在下载但还没有进度"集合
                                            downloadingButNoProgress = downloadingButNoProgress + sound.id
                                            // 开始下载
                                            val downloadFlow = cacheManager.downloadAudioWithProgress(
                                                sound.remoteUrl,
                                                sound.id
                                            )
                                            downloadFlow.collect { progress ->
                                                when (progress) {
                                                    is org.xmsleep.app.audio.DownloadProgress.Progress -> {
                                                        val percent = progress.bytesRead.toFloat() / progress.contentLength
                                                        android.util.Log.d("StarSkyScreen", "下载进度: ${sound.id} = $percent")
                                                        // 收到第一个进度更新，从"正在下载但还没有进度"集合中移除
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            put(sound.id, percent)
                                                        }
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Success -> {
                                                        android.util.Log.d("StarSkyScreen", "下载完成: ${sound.id}")
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            remove(sound.id)
                                                        }
                                                        // 下载完成后，增加缓冲时间再播放
                                                        val uri = resourceManager.getSoundUri(sound)
                                                        if (uri != null) {
                                                            // 延迟200ms确保文件系统写入完成
                                                            delay(200)
                                                            audioManager.playRemoteSound(context, sound, uri)
                                                            playingSounds = playingSounds + sound.id
                                                        } else {
                                                            android.util.Log.e("StarSkyScreen", "下载完成后无法获取URI: ${sound.id}")
                                                            Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                                        }
                                                        return@collect
                                                    }
                                                    is org.xmsleep.app.audio.DownloadProgress.Error -> {
                                                        downloadingButNoProgress = downloadingButNoProgress - sound.id
                                                        android.util.Log.e("StarSkyScreen", "下载失败: ${sound.id} - ${progress.exception.message}")
                                                        downloadingSounds = downloadingSounds.toMutableMap().apply {
                                                            remove(sound.id)
                                                        }
                                                        Toast.makeText(context, context.getString(R.string.download_failed) + ": ${progress.exception.message}", Toast.LENGTH_SHORT).show()
                                                        return@collect
                                                    }
                                                }
                                            }
                                        } else {
                                            // 已缓存或直接播放
                                            val uri = resourceManager.getSoundUri(sound)
                                            if (uri != null) {
                                                audioManager.playRemoteSound(context, sound, uri)
                                                playingSounds = playingSounds + sound.id
                                            } else {
                                                android.util.Log.e("StarSkyScreen", "无法获取URI: ${sound.id}")
                                                Toast.makeText(context, "播放失败: 无法获取音频文件", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("StarSkyScreen", "播放失败: ${e.message}")
                                        Toast.makeText(context, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onVolumeClick = {
                                selectedSoundForVolume = sound
                                volume = audioManager.getRemoteVolume(sound.id)
                                showVolumeDialog = true
                            }
                        )
                    }
                }
                }
            }
        }
        // 空状态
        else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Satellite,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
        Text(
            text = context.getString(R.string.star_sky_more_sounds_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
                }
            }
        }
        }
        
        // 音量调节弹窗
        if (showVolumeDialog && selectedSoundForVolume != null) {
            val sound = selectedSoundForVolume!!
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
                        
                        // 音量滑块
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = volume,
                                onValueChange = { 
                                    volume = it
                                    // 实时应用到该声音
                                    audioManager.setRemoteVolume(sound.id, volume)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                valueRange = 0f..1f,
                                steps = 19  // 0到100，步长5%
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
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showVolumeDialog = false
                        Toast.makeText(context, context.getString(R.string.volume_set_to, (volume * 100).toInt()), Toast.LENGTH_SHORT).show()
                    }) {
                        Text(context.getString(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVolumeDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
    }
}
