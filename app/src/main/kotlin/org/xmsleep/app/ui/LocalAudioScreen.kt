package org.xmsleep.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.LocalAudioPlayer
import org.xmsleep.app.timer.TimerManager

/**
 * 本地音频数据类
 */
data class LocalAudioFile(
    val id: Long,
    val title: String,
    val artist: String?,
    val duration: Long,
    val uri: Uri,
    val dateAdded: Long
)

/**
 * 本地音频页面
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocalAudioScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    // 使用更可靠的方式获取 Activity
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is androidx.activity.ComponentActivity) {
                return@remember ctx
            }
            ctx = ctx.baseContext
        }
        null
    }
    val scope = rememberCoroutineScope()
    val audioManager = remember { AudioManager.getInstance() }
    val timerManager = remember { TimerManager.getInstance() }
    val localAudioPlayer = remember { LocalAudioPlayer.getInstance() }
    
    // 获取需要的权限
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    // 权限状态（进入此页面时应该已经有权限了）
    val hasPermission = remember {
        ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
    }
    
    // 状态
    var localAudioList by remember { mutableStateOf<List<LocalAudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var selectedAudioForVolume by remember { mutableStateOf<LocalAudioFile?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedAudioForMenu by remember { mutableStateOf<LocalAudioFile?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    
    // 搜索相关状态
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // 过滤后的音频列表
    val filteredAudioList = remember(localAudioList, searchQuery) {
        if (searchQuery.isBlank()) {
            localAudioList
        } else {
            localAudioList.filter { audio ->
                audio.title.contains(searchQuery, ignoreCase = true) ||
                audio.artist?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    
    // 收藏的本地音频列表（保存 URI 字符串）
    var favoriteLocalAudios by remember { 
        mutableStateOf(
            org.xmsleep.app.preferences.PreferencesManager.getLocalAudioFavorites(context)
        )
    }
    
    // 本地音频媒体服务
    val mediaService = remember { org.xmsleep.app.audio.LocalAudioMediaService.getInstance(context) }
    
    // 扫描音频文件的函数
    val scanAudioFiles: (isRefresh: Boolean) -> Unit = { isRefresh ->
        if (isRefresh) {
            isRefreshing = true
        } else {
            isLoading = true
        }
        scope.launch {
            // 如果是刷新，添加一个小延迟让动画更流畅
            if (isRefresh) {
                kotlinx.coroutines.delay(300)
            }
            
            withContext(Dispatchers.IO) {
                try {
                    val audioFiles = mutableListOf<LocalAudioFile>()
                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    
                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,  // 使用 DISPLAY_NAME 而不是 TITLE
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATE_ADDED
                    )
                    
                    val selection: String? = null
                    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                    
                    context.contentResolver.query(
                        collection,
                        projection,
                        selection,
                        null,
                        sortOrder
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val displayName = cursor.getString(displayNameColumn)
                            // 从 DISPLAY_NAME 中移除扩展名作为 title
                            val title = displayName.substringBeforeLast(".")
                            val artist = cursor.getString(artistColumn)
                            val duration = cursor.getLong(durationColumn)
                            val dateAdded = cursor.getLong(dateColumn)
                            val uri = ContentUris.withAppendedId(collection, id)
                            
                            audioFiles.add(
                                LocalAudioFile(
                                    id = id,
                                    title = title,
                                    artist = artist,
                                    duration = duration,
                                    uri = uri,
                                    dateAdded = dateAdded
                                )
                            )
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        localAudioList = audioFiles
                        isLoading = false
                        isRefreshing = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocalAudioScreen", "扫描音频文件失败", e)
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        isRefreshing = false
                    }
                }
            }
        }
    }
    
    // 切换收藏状态
    fun toggleFavorite(audio: LocalAudioFile) {
        val uriString = audio.uri.toString()
        val newFavorites = if (favoriteLocalAudios.contains(uriString)) {
            favoriteLocalAudios - uriString
        } else {
            favoriteLocalAudios + uriString
        }
        favoriteLocalAudios = newFavorites
        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFavorites(context, newFavorites)
        
        val message = if (newFavorites.contains(uriString)) {
            context.getString(R.string.added_to_favorite)
        } else {
            context.getString(R.string.removed_from_favorite)
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    // 删除音频文件
    fun deleteAudioFile(audio: LocalAudioFile) {
        scope.launch {
            val success = mediaService.deleteMedia(audio.uri)
            if (success) {
                // 立即从列表中移除，无需等待扫描
                localAudioList = localAudioList.filter { it.id != audio.id }
                // 如果该音频在收藏中，也移除
                val uriString = audio.uri.toString()
                if (favoriteLocalAudios.contains(uriString)) {
                    favoriteLocalAudios = favoriteLocalAudios - uriString
                    org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioFavorites(context, favoriteLocalAudios)
                }
                android.widget.Toast.makeText(context, context.getString(R.string.delete_success), android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, context.getString(R.string.delete_cancelled), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // 重命名状态
    var isRenaming by remember { mutableStateOf(false) }
    
    // 重命名音频文件
    fun renameAudioFile(audio: LocalAudioFile, newName: String) {
        scope.launch {
            isRenaming = true
            
            // 确保新名称包含文件扩展名
            val finalName = if (!newName.contains(".")) {
                // 从原文件名中提取扩展名
                val extension = audio.title.substringAfterLast(".", "")
                if (extension.isNotEmpty()) {
                    "$newName.$extension"
                } else {
                    newName
                }
            } else {
                newName
            }
            
            android.util.Log.d("LocalAudioScreen", "开始重命名: ${audio.title} -> $finalName, URI: ${audio.uri}")
            val success = mediaService.renameMedia(audio.uri, finalName)
            android.util.Log.d("LocalAudioScreen", "重命名结果: $success")
            
            if (success) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.rename_success), android.widget.Toast.LENGTH_SHORT).show()
                }
                
                // 关键：延迟足够长的时间让 MediaStore 更新
                kotlinx.coroutines.delay(1000)
                
                // 强制刷新列表
                withContext(Dispatchers.Main) {
                    scanAudioFiles(false)  // 使用 false 避免触发下拉刷新动画
                }
            } else {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.getString(R.string.rename_failed), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            isRenaming = false
        }
    }
    
    // 从单例获取播放状态
    val playingAudioIds by localAudioPlayer.playingAudioIds.collectAsState()
    val playingAudioId by localAudioPlayer.playingAudioId.collectAsState()
    val currentVolume by localAudioPlayer.currentVolume.collectAsState()
    
    // 倒计时监听器
    val timerListener = remember {
        object : TimerManager.TimerListener {
            override fun onTimerTick(timeLeftMillis: Long) {}
            
            override fun onTimerFinished() {
                localAudioPlayer.stopAllAudios()
                if (!audioManager.hasAnyPlayingSounds()) {
                    audioManager.stopMusicService(context)
                }
            }
            
            override fun onTimerCancelled() {}
        }
    }
    
    // 添加和移除倒计时监听器
    DisposableEffect(Unit) {
        timerManager.addListener(timerListener)
        onDispose {
            timerManager.removeListener(timerListener)
        }
    }
    // 初始权限检查
    LaunchedEffect(Unit) {
        android.util.Log.d("LocalAudioScreen", "开始检查权限")
        android.util.Log.d("LocalAudioScreen", "权限状态: hasPermission=$hasPermission")
        
        if (hasPermission) {
            android.util.Log.d("LocalAudioScreen", "权限已授予，开始扫描")
            scanAudioFiles(false)
        } else {
            // 未授予权限，显示引导界面
            android.util.Log.d("LocalAudioScreen", "权限未授予，显示引导界面")
            isLoading = false
        }
    }
    
    // 监听权限状态变化，授予后自动扫描
    LaunchedEffect(hasPermission) {
        if (hasPermission && localAudioList.isEmpty()) {
            android.util.Log.d("LocalAudioScreen", "权限已授予，触发扫描")
            scanAudioFiles(false)
        }
    }
    
    // 搜索框焦点请求器
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    
    // 当进入搜索模式时，自动聚焦搜索框
    LaunchedEffect(isSearching) {
        if (isSearching) {
            kotlinx.coroutines.delay(100) // 延迟一点确保UI已渲染
            focusRequester.requestFocus()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text(
                            context.getString(R.string.local_audio),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        },
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
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = context.getString(R.string.search_audio),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(
                    WindowInsets.systemBars.union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top)
                )
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = context.getString(R.string.scanning_local_audio),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                !hasPermission -> {
                    // 理论上不应该到这里，因为权限检查在进入页面前就完成了
                    // 如果到这里，说明权限在进入页面后被撤销了
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            EmptyStateAnimation(size = 240.dp)
                            Text(
                                text = context.getString(R.string.storage_permission_required),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = "请返回主页面重新授权",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onBack) {
                                Text(context.getString(R.string.back))
                            }
                        }
                    }
                }
                localAudioList.isEmpty() && !isSearching -> {
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
                                text = context.getString(R.string.no_local_audio),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = context.getString(R.string.no_audio_files_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (filteredAudioList.isEmpty() && searchQuery.isNotBlank()) {
                            // 搜索无结果
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = context.getString(R.string.no_search_results),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "\"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
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
                                    items(
                                        items = filteredAudioList, 
                                        key = { it.id }
                                    ) { audio ->
                                    LocalAudioItem(
                                        audio = audio,
                                        isPlaying = playingAudioIds.contains(audio.id),
                                        modifier = Modifier.animateItem(),
                                        onCardClick = {
                                            // 切换播放状态
                                            localAudioPlayer.toggleAudio(
                                                context = context,
                                                audioId = audio.id,
                                                audioUri = audio.uri,
                                                onError = { message ->
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        message,
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                            
                                            // 管理音乐服务
                                            if (localAudioPlayer.hasActiveAudio() || audioManager.hasAnyPlayingSounds()) {
                                                audioManager.startMusicService(context)
                                            } else {
                                                audioManager.stopMusicService(context)
                                            }
                                        },
                                        onVolumeClick = {
                                            selectedAudioForVolume = audio
                                            showVolumeDialog = true
                                        },
                                        onLongPress = {
                                            selectedAudioForMenu = audio
                                            showContextMenu = true
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
        
        // 重命名时的全屏遮罩层（在 Scaffold 外面，可以遮住整个页面包括 TopAppBar）
        if (isRenaming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = context.getString(R.string.renaming),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
        
        // 音量调节弹窗
        if (showVolumeDialog && selectedAudioForVolume != null) {
            val audio = selectedAudioForVolume!!
            var volume by remember(audio.id) { mutableStateOf(localAudioPlayer.getVolume(audio.id)) }
            
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text(audio.title) },
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
                            value = volume,
                            onValueChange = { 
                                volume = it
                                localAudioPlayer.setVolume(audio.id, it)
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
                },
                dismissButton = {
                    TextButton(onClick = { showVolumeDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        // 长按菜单 - 底部弹窗
        if (showContextMenu && selectedAudioForMenu != null) {
            val isFavorite = favoriteLocalAudios.contains(selectedAudioForMenu!!.uri.toString())
            val sheetState = rememberModalBottomSheetState()
            
            ModalBottomSheet(
                onDismissRequest = { showContextMenu = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // 标题
                    Text(
                        text = selectedAudioForMenu!!.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 收藏选项
                    Surface(
                        onClick = {
                            toggleFavorite(selectedAudioForMenu!!)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isFavorite) {
                                    context.getString(R.string.remove_from_favorite)
                                } else {
                                    context.getString(R.string.add_to_favorite)
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    // 重命名选项
                    Surface(
                        onClick = {
                            showContextMenu = false
                            renameText = selectedAudioForMenu!!.title
                            showRenameDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DriveFileRenameOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = context.getString(R.string.rename),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    // 删除选项
                    Surface(
                        onClick = {
                            showContextMenu = false
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = context.getString(R.string.delete),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        // 重命名对话框
        if (showRenameDialog && selectedAudioForMenu != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text(context.getString(R.string.rename)) },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(context.getString(R.string.new_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (renameText.isNotBlank() && renameText != selectedAudioForMenu!!.title) {
                                // 如果正在播放，先停止
                                if (localAudioPlayer.isAudioPlaying(selectedAudioForMenu!!.id)) {
                                    localAudioPlayer.stopAudio(selectedAudioForMenu!!.id)
                                    if (!localAudioPlayer.hasActiveAudio() && !audioManager.hasAnyPlayingSounds()) {
                                        audioManager.stopMusicService(context)
                                    }
                                }
                                renameAudioFile(selectedAudioForMenu!!, renameText)
                            }
                            showRenameDialog = false
                        }
                    ) {
                        Text(context.getString(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        // 删除确认对话框
        if (showDeleteDialog && selectedAudioForMenu != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(context.getString(R.string.confirm_delete)) },
                text = { Text(context.getString(R.string.confirm_delete_message, selectedAudioForMenu!!.title)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 如果正在播放，先停止
                            if (localAudioPlayer.isAudioPlaying(selectedAudioForMenu!!.id)) {
                                localAudioPlayer.stopAudio(selectedAudioForMenu!!.id)
                                if (!localAudioPlayer.hasActiveAudio() && !audioManager.hasAnyPlayingSounds()) {
                                    audioManager.stopMusicService(context)
                                }
                            }
                            deleteAudioFile(selectedAudioForMenu!!)
                            showDeleteDialog = false
                        }
                    ) {
                        Text(context.getString(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalAudioItem(
    audio: LocalAudioFile,
    isPlaying: Boolean,
    onCardClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = audio.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        audio.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Text(
                            text = formatDuration(audio.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AudioVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier.size(24.dp, 16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = onVolumeClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "调节音量",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
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
    return String.format("%d:%02d", minutes, remainingSeconds)
}
