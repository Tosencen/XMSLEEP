package org.xmsleep.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
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

data class LocalAudioFile(
    val id: Long,
    val title: String,
    val artist: String?,
    val duration: Long,
    val uri: Uri,
    val dateAdded: Long
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val filteredAudioList = remember(localAudioList, searchQuery) {
        if (searchQuery.isBlank()) localAudioList
        else localAudioList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    var favoriteLocalAudios by remember {
        mutableStateOf(org.xmsleep.app.preferences.PreferencesManager.getLocalAudioFavorites(context))
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
                    val projection = arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATE_ADDED
                    )
                    val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
                    context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idCol)
                            val name = cursor.getString(nameCol)
                            audioFiles.add(LocalAudioFile(
                                id = id,
                                title = name.substringBeforeLast("."),
                                artist = cursor.getString(artistCol),
                                duration = cursor.getLong(durCol),
                                uri = ContentUris.withAppendedId(collection, id),
                                dateAdded = cursor.getLong(dateCol)
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
                val ext = audio.title.substringAfterLast(".", "")
                if (ext.isNotEmpty()) "$newName.$ext" else newName
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

    val playingAudioIds by localAudioPlayer.playingAudioIds.collectAsState()
    val currentPlayMode by localAudioPlayer.playMode.collectAsState()

    val timerListener = remember {
        object : TimerManager.TimerListener {
            override fun onTimerTick(timeLeftMillis: Long) {}
            override fun onTimerFinished(durationMinutes: Int) {
                localAudioPlayer.stopAllAudios()
                if (!audioManager.hasAnyPlayingSounds()) audioManager.stopMusicService(context)
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
    LaunchedEffect(localAudioList) {
        if (localAudioList.isNotEmpty()) localAudioPlayer.setPlaylist(localAudioList.map { it.id to it.uri })
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
                            IconButton(onClick = { localAudioPlayer.cyclePlayMode(context) }) {
                                Icon(
                                    imageVector = when (currentPlayMode) {
                                        PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                                        PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                                        PlayMode.SEQUENTIAL -> Icons.Filled.Repeat
                                    },
                                    contentDescription = when (currentPlayMode) {
                                        PlayMode.SHUFFLE -> "随机播放"
                                        PlayMode.REPEAT_ONE -> "单曲循环"
                                        PlayMode.SEQUENTIAL -> "顺序播放"
                                    },
                                    tint = if (currentPlayMode == PlayMode.SEQUENTIAL) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                )
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
                                Text("请返回主页面重新授权", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (filteredAudioList.isEmpty() && searchQuery.isNotBlank()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                        Text(context.getString(R.string.no_search_results), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("\"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
                        audio.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)) }
                        Text(formatDuration(audio.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                val progressFraction = if (totalDuration > 0) currentProgress.toFloat() / totalDuration.toFloat() else 0f
                Slider(value = progressFraction, onValueChange = { localAudioPlayer.seekTo(audio.id, (it * totalDuration).toInt()); currentProgress = (it * totalDuration).toInt() }, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(currentProgress.toLong()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioVisualizer(isPlaying = isPlaying, modifier = Modifier.size(24.dp, 16.dp), color = MaterialTheme.colorScheme.primary)
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
