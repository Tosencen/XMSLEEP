package org.xmsleep.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.LocalAudioPlayer
import org.xmsleep.app.audio.PlayMode
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.utils.Logger
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class LocalAudioFile(
    val id: Long,
    val title: String,
    val extension: String,
    val artist: String?,
    val duration: Long,
    val uri: Uri,
    val dateAdded: Long,
    /** 所在文件夹（API 29+ 为相对路径如 "Music/白噪音"，以下为绝对目录），未知时为 UNKNOWN_FOLDER_KEY */
    val folderPath: String,
    val size: Long
)

/** 未知文件夹的占位 key（避免与真实路径冲突） */
private const val UNKNOWN_FOLDER_KEY = "~unknown~"

/** 收藏筛选的占位 key（选中时按收藏列表过滤） */
private const val FAVORITES_FOLDER_KEY = "~favorites~"

/** 文件夹过滤：单选，空值表示全部 */
private data class FolderEntry(val path: String, val displayName: String, val count: Int)

/** 时长过滤选项（DURATION 未知即 <=0 时，只有 ALL 会包含） */
enum class LocalAudioDurationFilter {
    ALL, UNDER_1_MIN, MIN_1_3, MIN_3_10, OVER_10_MIN;

    fun matches(durationMs: Long): Boolean = when (this) {
        ALL -> true
        UNDER_1_MIN -> durationMs in 1 until 60_000L
        MIN_1_3 -> durationMs in 60_000L until 180_000L
        MIN_3_10 -> durationMs in 180_000L until 600_000L
        OVER_10_MIN -> durationMs >= 600_000L
    }
}

/** 排序选项 */
enum class LocalAudioSortOption {
    DATE_DESC, NAME_ASC, DURATION_ASC, DURATION_DESC, SIZE_DESC;

    fun comparator(): Comparator<LocalAudioFile> = when (this) {
        DATE_DESC -> compareByDescending { it.dateAdded }
        NAME_ASC -> compareBy { it.title.lowercase() }
        DURATION_ASC -> compareBy { it.duration }
        DURATION_DESC -> compareByDescending { it.duration }
        SIZE_DESC -> compareByDescending { it.size }
    }
}

/** 系统默认文件夹（小写比较）：默认隐藏，需在“管理文件夹”中勾选收录 */
private val SYSTEM_FOLDERS = setOf(
    "recordings", "ringtones", "notifications", "alarms", "podcasts",
    "call recordings", "voice recorder", "whatsapp audio"
)

/**
 * 文件夹是否应被隐藏（系统默认文件夹或隐藏目录），供文件夹列表与测试复用。
 */
internal fun isHiddenSystemFolder(displayName: String): Boolean {
    val name = displayName.lowercase()
    return displayName.startsWith(".") || name in SYSTEM_FOLDERS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LocalAudioScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { AudioManager.getInstance() }
    val timerManager = remember { TimerManager.getInstance() }
    val localAudioPlayer = remember { LocalAudioPlayer.getInstance() }
    LaunchedEffect(Unit) { localAudioPlayer.initPlayMode(context) }

    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
    }

    var localAudioList by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var selectedAudioForVolume by remember { mutableStateOf<LocalAudioFile?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedAudioForMenu by remember { mutableStateOf<LocalAudioFile?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // 过滤与排序状态（持久化，退出再进保持上次选择）
    var selectedFolder by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioFilterFolder(context).ifEmpty { null })
    }
    var durationFilter by remember {
        mutableStateOf(
            runCatching {
                LocalAudioDurationFilter.valueOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioFilterDuration(context))
            }.getOrDefault(LocalAudioDurationFilter.ALL)
        )
    }
    var sortOption by remember {
        mutableStateOf(
            runCatching {
                LocalAudioSortOption.valueOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioSort(context))
            }.getOrDefault(LocalAudioSortOption.DATE_DESC)
        )
    }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFolderManager by remember { mutableStateOf(false) }
    // 系统文件夹中“勾选收录”的集合（默认隐藏，勾选后显示）
    var enabledFolders by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioEnabledFolders(context))
    }
    // 普通文件夹中“主动隐藏”的集合（默认显示，取消勾选后隐藏）
    var hiddenFolders by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioHiddenFolders(context))
    }

    fun selectFolder(folder: String?) {
        selectedFolder = folder
        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFilterFolder(context, folder ?: "")
    }

    fun selectDuration(filter: LocalAudioDurationFilter) {
        durationFilter = filter
        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFilterDuration(context, filter.name)
    }

    fun selectSort(option: LocalAudioSortOption) {
        sortOption = option
        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioSort(context, option.name)
    }

    // 文件夹是否显示：系统目录→勾选收录才显示；普通目录→未主动隐藏即显示
    fun isFolderShown(path: String, displayName: String): Boolean {
        return if (isHiddenSystemFolder(displayName)) path in enabledFolders else path !in hiddenFolders
    }

    // 文件夹列表（仅显示可见文件夹），按名称排序
    val folderEntries = remember(localAudioList, context, enabledFolders, hiddenFolders) {
        val unknownLabel = context.getString(R.string.unknown_folder)
        localAudioList
            .groupBy { it.folderPath }
            .mapNotNull { (path, files) ->
                val displayName = if (path == UNKNOWN_FOLDER_KEY) unknownLabel else path.substringAfterLast('/')
                if (isFolderShown(path, displayName)) FolderEntry(path, displayName, files.size) else null
            }
            .sortedBy { it.displayName.lowercase() }
    }

    // 全部文件夹列表（管理弹窗用，包含系统默认文件夹），按名称排序
    val allFolderEntries = remember(localAudioList, context) {
        val unknownLabel = context.getString(R.string.unknown_folder)
        localAudioList
            .groupBy { it.folderPath }
            .map { (path, files) ->
                val displayName = if (path == UNKNOWN_FOLDER_KEY) unknownLabel else path.substringAfterLast('/')
                FolderEntry(path, displayName, files.size)
            }
            .sortedBy { it.displayName.lowercase() }
    }

    fun toggleFolderVisibility(path: String) {
        val name = if (path == UNKNOWN_FOLDER_KEY) "" else path.substringAfterLast('/')
        if (isHiddenSystemFolder(name)) {
            val newSet = if (path in enabledFolders) enabledFolders - path else enabledFolders + path
            enabledFolders = newSet
            org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioEnabledFolders(context, newSet)
        } else {
            val newSet = if (path in hiddenFolders) hiddenFolders - path else hiddenFolders + path
            hiddenFolders = newSet
            org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioHiddenFolders(context, newSet)
        }
        // 若隐藏的是当前选中的文件夹，列表会清空，重置为“全部”
        if (selectedFolder == path) selectFolder(null)
    }

    var favoriteLocalAudios by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioFavorites(context))
    }

    // 组合过滤：文件夹显隐 ∩ 收藏(伪文件夹) ∩ 文件夹 ∩ 时长 ∩ 搜索关键词，再排序
    val filteredAudioList = remember(localAudioList, searchQuery, selectedFolder, durationFilter, sortOption, enabledFolders, hiddenFolders, favoriteLocalAudios) {
        localAudioList
            .filter { audio ->
                val name = if (audio.folderPath == UNKNOWN_FOLDER_KEY) "" else audio.folderPath.substringAfterLast('/')
                if (isHiddenSystemFolder(name)) audio.folderPath in enabledFolders else audio.folderPath !in hiddenFolders
            }
            .filter {
                searchQuery.isBlank() ||
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist?.contains(searchQuery, ignoreCase = true) == true
            }
            .filter { audio ->
                if (selectedFolder == FAVORITES_FOLDER_KEY) {
                    favoriteLocalAudios.contains(audio.uri.toString())
                } else {
                    selectedFolder == null || audio.folderPath == selectedFolder
                }
            }
            .filter { durationFilter.matches(it.duration) }
            .sortedWith(sortOption.comparator())
    }

    val mediaService = remember { org.xmsleep.app.audio.LocalAudioMediaService.getInstance(context) }

    val scanAudioFiles: (Boolean) -> Unit = { isRefresh ->
        if (isRefresh) isRefreshing = true else isLoading = true
        scope.launch {
            if (isRefresh) delay(300)
            withContext(Dispatchers.IO) {
                try {
                    val audioFiles = mutableListOf<LocalAudioFile>()
                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.RELATIVE_PATH
                    } else {
                        MediaStore.Audio.Media.DATA
                    }
                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.SIZE,
                        pathColumn
                    )
                    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                    context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                        val folderCol = cursor.getColumnIndexOrThrow(pathColumn)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = cursor.getString(nameCol)
                            val folderPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val relativePath = cursor.getString(folderCol)
                                if (relativePath.isNullOrBlank()) UNKNOWN_FOLDER_KEY else relativePath.trimEnd('/')
                            } else {
                                val data = cursor.getString(folderCol)
                                if (data.isNullOrBlank()) UNKNOWN_FOLDER_KEY
                                else data.substringBeforeLast('/', "").ifEmpty { UNKNOWN_FOLDER_KEY }
                            }
                            audioFiles.add(LocalAudioFile(
                                id = id,
                                title = name.substringBeforeLast("."),
                                extension = name.substringAfterLast(".", ""),
                                artist = cursor.getString(artistCol),
                                duration = cursor.getLong(durCol),
                                uri = ContentUris.withAppendedId(collection, id),
                                dateAdded = cursor.getLong(dateCol),
                                folderPath = folderPath,
                                size = cursor.getLong(sizeCol)
                            ))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        localAudioList = audioFiles
                        isLoading = false
                        isRefreshing = false
                    }
                } catch (e: Exception) {
                    Logger.e("LocalAudioScreen", "扫描音频文件失败", e)
                    withContext(Dispatchers.Main) { isLoading = false; isRefreshing = false }
                }
            }
        }
    }

    fun toggleFavorite(audio: LocalAudioFile) {
        val uriStr = audio.uri.toString()
        val newFavs = if (favoriteLocalAudios.contains(uriStr)) favoriteLocalAudios - uriStr else favoriteLocalAudios + uriStr
        favoriteLocalAudios = newFavs
        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFavorites(context, newFavs)
        val msg = if (newFavs.contains(uriStr)) context.getString(R.string.added_to_favorite) else context.getString(R.string.removed_from_favorite)
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun deleteAudioFile(audio: LocalAudioFile) {
        scope.launch {
            val success = mediaService.deleteMedia(audio.uri)
            if (success) {
                localAudioList = localAudioList.filter { it.id != audio.id }
                val uriStr = audio.uri.toString()
                if (favoriteLocalAudios.contains(uriStr)) {
                    favoriteLocalAudios = favoriteLocalAudios - uriStr
                    org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFavorites(context, favoriteLocalAudios)
                }
                android.widget.Toast.makeText(context, context.getString(R.string.delete_success), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.delete_cancelled), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isRenaming by remember { mutableStateOf(false) }

    fun renameAudioFile(audio: LocalAudioFile, newName: String) {
        scope.launch {
            isRenaming = true
            val finalName = if (!newName.contains(".")) {
                if (audio.extension.isNotEmpty()) "$newName.${audio.extension}" else newName
            } else newName
            val success = mediaService.renameMedia(audio.uri, finalName)
            if (success) {
                withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, context.getString(R.string.rename_success), android.widget.Toast.LENGTH_SHORT).show() }
                delay(1000)
                withContext(Dispatchers.Main) { scanAudioFiles(false) }
            } else {
                withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, context.getString(R.string.rename_failed), android.widget.Toast.LENGTH_SHORT).show() }
            }
            isRenaming = false
        }
    }

    val playingAudioIds by localAudioPlayer.playingAudioIds.collectAsStateWithLifecycle()
    val currentPlayMode by localAudioPlayer.playMode.collectAsStateWithLifecycle()

    val timerListener = remember {
        object : TimerManager.TimerListener {
            override fun onTimerTick(timeLeftMillis: Long) {}
            override fun onTimerFinished(durationMinutes: Int) {
                // 定时到点：本地音频淡出 30 秒后停止，避免突然中断
                localAudioPlayer.fadeOutAndStopAll()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!audioManager.hasAnyPlayingSounds()) audioManager.stopMusicService(context)
                }, 32_000L)
            }
            override fun onTimerCancelled() {}
        }
    }

    DisposableEffect(Unit) {
        timerManager.addListener(timerListener)
        onDispose { timerManager.removeListener(timerListener) }
    }

    LaunchedEffect(Unit) {
        if (hasPermission) scanAudioFiles(false) else isLoading = false
    }
    // 播放列表跟随当前过滤/搜索后的可见列表（切过滤时正在播的歌不打断，下一首从新列表取）
    LaunchedEffect(filteredAudioList) {
        if (filteredAudioList.isNotEmpty()) localAudioPlayer.setPlaylist(filteredAudioList.map { it.id to it.uri })
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) { delay(100); focusRequester.requestFocus() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(context.getString(R.string.search_audio)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        } else {
                            Text(context.getString(R.string.local_audio), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (isSearching) { isSearching = false; searchQuery = "" } else onBack() },
                            modifier = Modifier.offset(x = (-4).dp)
                        ) {
                            Box(Modifier.size(24.dp)) {
                                Icon(
                                    if (isSearching) Icons.Filled.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = if (isSearching) context.getString(R.string.cancel) else context.getString(R.string.go_back),
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        Icons.Filled.Sort,
                                        contentDescription = context.getString(R.string.sort_by),
                                        tint = if (sortOption == LocalAudioSortOption.DATE_DESC) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    LocalAudioSortOption.entries.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(sortOptionLabel(context, opt)) },
                                            onClick = { selectSort(opt); showSortMenu = false },
                                            trailingIcon = if (sortOption == opt) {
                                                { Icon(Icons.Filled.Check, contentDescription = null) }
                                            } else null
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Filled.Search, contentDescription = context.getString(R.string.search_audio), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator()
                                Text(context.getString(R.string.scanning_local_audio), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    !hasPermission -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 32.dp)) {
                                EmptyStateAnimation(animationSize = 240.dp)
                                Text(context.getString(R.string.storage_permission_required), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Text(context.getString(R.string.go_home_to_grant_permission), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onBack) { Text(context.getString(R.string.back)) }
                            }
                        }
                    }
                    localAudioList.isEmpty() && !isSearching -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                EmptyStateAnimation(animationSize = 240.dp)
                                Text(context.getString(R.string.no_local_audio), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(context.getString(R.string.no_audio_files_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 文件夹 + 时长筛选（直接展示在列表顶部）
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilterSectionLabel(
                                            text = context.getString(R.string.filter_folder_section),
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(
                                            onClick = { showFolderManager = true },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Folder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(context.getString(R.string.manage_folders))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedFolder == FAVORITES_FOLDER_KEY,
                                            onClick = { selectFolder(if (selectedFolder == FAVORITES_FOLDER_KEY) null else FAVORITES_FOLDER_KEY) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Favorite,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                                    tint = if (selectedFolder == FAVORITES_FOLDER_KEY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            label = { Text("${context.getString(R.string.favorites)} ${favoriteLocalAudios.size}") }
                                        )
                                        FilterChip(
                                            selected = selectedFolder == null,
                                            onClick = { selectFolder(null) },
                                            label = { Text(context.getString(R.string.filter_all)) }
                                        )
                                        folderEntries.forEach { entry ->
                                            FilterChip(
                                                selected = selectedFolder == entry.path,
                                                onClick = { selectFolder(if (selectedFolder == entry.path) null else entry.path) },
                                                label = { Text("${entry.displayName} ${entry.count}") }
                                            )
                                        }
                                    }
                                }
                                Column {
                                    FilterSectionLabel(text = context.getString(R.string.filter_duration_section))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LocalAudioDurationFilter.entries.forEach { opt ->
                                            FilterChip(
                                                selected = durationFilter == opt,
                                                onClick = { selectDuration(opt) },
                                                label = { Text(durationFilterLabel(context, opt)) }
                                            )
                                        }
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (filteredAudioList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                                            if (searchQuery.isNotBlank()) {
                                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                Text(context.getString(R.string.no_search_results), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("\"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                            } else {
                                                Icon(Icons.Filled.FilterAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                Text(context.getString(R.string.no_matching_audio), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                            }
                                        }
                                    }
                                } else {
                                    PullToRefreshBox(
                                        isRefreshing = isRefreshing,
                                        onRefresh = { scanAudioFiles(true) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(items = filteredAudioList, key = { it.id }) { audio ->
                                                LocalAudioItem(
                                                    audio = audio,
                                                    isPlaying = playingAudioIds.contains(audio.id),
                                                    playMode = currentPlayMode,
                                                    onCyclePlayMode = { localAudioPlayer.cyclePlayMode(context) },
                                                    modifier = Modifier.animateItem(),
                                                    onCardClick = {
                                                        localAudioPlayer.toggleAudio(
                                                            context = context,
                                                            audioId = audio.id,
                                                            audioUri = audio.uri,
                                                            onError = { msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show() }
                                                        )
                                                        if (localAudioPlayer.hasActiveAudio() || audioManager.hasAnyPlayingSounds()) {
                                                            audioManager.startMusicService(context)
                                                        } else {
                                                            audioManager.stopMusicService(context)
                                                        }
                                                    },
                                                    onVolumeClick = { selectedAudioForVolume = audio; showVolumeDialog = true },
                                                    onLongPress = { selectedAudioForMenu = audio; showContextMenu = true }
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
        }

        if (isRenaming) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(context.getString(R.string.renaming), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    // Volume dialog
    if (showVolumeDialog && selectedAudioForVolume != null) {
        val audio = selectedAudioForVolume!!
        var volume by remember(audio.id) { mutableStateOf(localAudioPlayer.getVolume(audio.id)) }
        AlertDialog(
            onDismissRequest = { showVolumeDialog = false },
            title = { Text(audio.title) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(context.getString(R.string.adjust_volume), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = volume, onValueChange = { volume = it; localAudioPlayer.setVolume(audio.id, it) }, modifier = Modifier.fillMaxWidth(), valueRange = 0f..1f, steps = 19)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text("100%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVolumeDialog = false }) { Text(context.getString(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { showVolumeDialog = false }) { Text(context.getString(R.string.cancel)) } }
        )
    }

    // Context menu
    if (showContextMenu && selectedAudioForMenu != null) {
        val isFavorite = favoriteLocalAudios.contains(selectedAudioForMenu!!.uri.toString())
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showContextMenu = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(selectedAudioForMenu!!.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Surface(onClick = { toggleFavorite(selectedAudioForMenu!!); showContextMenu = false }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(if (isFavorite) context.getString(R.string.remove_from_favorite) else context.getString(R.string.add_to_favorite), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(onClick = { showContextMenu = false; renameText = selectedAudioForMenu!!.title; showRenameDialog = true }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text(context.getString(R.string.rename), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Surface(onClick = { showContextMenu = false; showDeleteDialog = true }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(context.getString(R.string.delete), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog && selectedAudioForMenu != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(context.getString(R.string.rename)) },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { if (renameText.isNotBlank()) renameAudioFile(selectedAudioForMenu!!, renameText); showRenameDialog = false }) { Text(context.getString(android.R.string.ok)) } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text(context.getString(R.string.cancel)) } }
        )
    }

    // Delete dialog
    if (showDeleteDialog && selectedAudioForMenu != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(context.getString(R.string.confirm_delete)) },
            text = { Text(context.getString(R.string.confirm_delete_message, selectedAudioForMenu!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    if (localAudioPlayer.isAudioPlaying(selectedAudioForMenu!!.id)) {
                        localAudioPlayer.stopAudio(selectedAudioForMenu!!.id)
                        if (!localAudioPlayer.hasActiveAudio() && !audioManager.hasAnyPlayingSounds()) audioManager.stopMusicService(context)
                    }
                    deleteAudioFile(selectedAudioForMenu!!)
                    showDeleteDialog = false
                }) { Text(context.getString(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(context.getString(R.string.cancel)) } }
        )
    }

    // 文件夹管理弹层：勾选要收录的文件夹（白名单）
    if (showFolderManager) {
        ModalBottomSheet(
            onDismissRequest = { showFolderManager = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                // 标题栏
                Text(
                    text = context.getString(R.string.manage_folders),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = context.getString(R.string.manage_folders_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                FilterSectionLabel(text = context.getString(R.string.manage_folders_system_section))
                val systemFolderEntries = allFolderEntries.filter { isHiddenSystemFolder(it.displayName) }
                if (systemFolderEntries.isEmpty()) {
                    Text(
                        text = context.getString(R.string.manage_folders_no_system_audio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        systemFolderEntries.forEach { entry ->
                            FolderManagerRow(
                                displayName = entry.displayName,
                                count = entry.count,
                                checked = entry.path in enabledFolders,
                                onClick = { toggleFolderVisibility(entry.path) }
                            )
                        }
                    }
                }
                FilterSectionLabel(
                    text = context.getString(R.string.manage_folders_other_section),
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column {
                    allFolderEntries
                        .filterNot { isHiddenSystemFolder(it.displayName) }
                        .forEach { entry ->
                            FolderManagerRow(
                                displayName = entry.displayName,
                                count = entry.count,
                                checked = entry.path !in hiddenFolders,
                                onClick = { toggleFolderVisibility(entry.path) }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun FolderManagerRow(
    displayName: String,
    count: Int,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onClick() }
            )
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalAudioItem(
    audio: LocalAudioFile,
    isPlaying: Boolean,
    playMode: PlayMode,
    onCyclePlayMode: () -> Unit,
    onCardClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localAudioPlayer = remember { LocalAudioPlayer.getInstance() }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "card_alpha"
    )

    var currentProgress by remember { mutableIntStateOf(0) }
    var totalDuration by remember { mutableIntStateOf(audio.duration.toInt()) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val progress = localAudioPlayer.getAudioProgress(audio.id)
                if (progress != null) { currentProgress = progress.first; totalDuration = progress.second }
                delay(500)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(cardAlpha)
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onCardClick, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(audio.title, style = MaterialTheme.typography.titleMedium, fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val folderName = if (audio.folderPath == UNKNOWN_FOLDER_KEY) "" else audio.folderPath.substringAfterLast('/')
                        val metaText = when {
                            audio.artist != null && folderName.isNotEmpty() -> "${audio.artist} · $folderName"
                            audio.artist != null -> audio.artist
                            folderName.isNotEmpty() -> folderName
                            else -> ""
                        }
                        if (metaText.isNotEmpty()) {
                            Text(metaText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        }
                        Text(
                            listOf(formatDuration(audio.duration), formatSize(audio.size)).filter { it.isNotEmpty() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                val progressFraction = if (totalDuration > 0) currentProgress.toFloat() / totalDuration.toFloat() else 0f
                me.saket.squiggles.SquigglySlider(
                    value = progressFraction,
                    onValueChange = {
                        localAudioPlayer.seekTo(audio.id, (it * totalDuration).toInt())
                        currentProgress = (it * totalDuration).toInt()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(currentProgress.toLong()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioVisualizer(isPlaying = isPlaying, modifier = Modifier.size(18.dp, 12.dp), color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = onCyclePlayMode, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = when (playMode) {
                                    PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                                    PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                                    PlayMode.SEQUENTIAL -> Icons.Filled.Repeat
                                },
                                contentDescription = when (playMode) {
                                    PlayMode.SHUFFLE -> "随机播放"
                                    PlayMode.REPEAT_ONE -> "单曲循环"
                                    PlayMode.SEQUENTIAL -> "顺序播放"
                                },
                                tint = if (playMode == PlayMode.SEQUENTIAL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onVolumeClick, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "调节音量", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / 1024f / 1024f
    return if (mb >= 100) {
        "${mb.toInt()} MB"
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

private fun durationFilterLabel(context: Context, filter: LocalAudioDurationFilter): String = when (filter) {
    LocalAudioDurationFilter.ALL -> context.getString(R.string.filter_duration_all)
    LocalAudioDurationFilter.UNDER_1_MIN -> context.getString(R.string.filter_duration_under_1min)
    LocalAudioDurationFilter.MIN_1_3 -> context.getString(R.string.filter_duration_1_3min)
    LocalAudioDurationFilter.MIN_3_10 -> context.getString(R.string.filter_duration_3_10min)
    LocalAudioDurationFilter.OVER_10_MIN -> context.getString(R.string.filter_duration_over_10min)
}

private fun sortOptionLabel(context: Context, option: LocalAudioSortOption): String = when (option) {
    LocalAudioSortOption.DATE_DESC -> context.getString(R.string.sort_date_desc)
    LocalAudioSortOption.NAME_ASC -> context.getString(R.string.sort_name_asc)
    LocalAudioSortOption.DURATION_ASC -> context.getString(R.string.sort_duration_asc)
    LocalAudioSortOption.DURATION_DESC -> context.getString(R.string.sort_duration_desc)
    LocalAudioSortOption.SIZE_DESC -> context.getString(R.string.sort_size_desc)
}
