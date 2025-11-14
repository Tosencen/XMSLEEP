package org.xmsleep.app.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.Constants
import org.xmsleep.app.ui.components.AboutDialog
import org.xmsleep.app.ui.components.ClearCacheDialog
import org.xmsleep.app.ui.components.LanguageSelectionDialog
import org.xmsleep.app.ui.components.SwitchItem
import org.xmsleep.app.update.UpdateDialog
import org.xmsleep.app.utils.*

/**
 * 设置页面 - 应用配置和管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hideAnimation: Boolean = true,
    onHideAnimationChange: (Boolean) -> Unit = {},
    updateViewModel: org.xmsleep.app.update.UpdateViewModel,
    currentLanguage: LanguageManager.Language,
    onLanguageChange: (LanguageManager.Language) -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToSounds: () -> Unit = {},
    pinnedSounds: MutableState<MutableSet<AudioManager.Sound>>,
    favoriteSounds: MutableState<MutableSet<AudioManager.Sound>>,
    onScrollDetected: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioManager = remember { org.xmsleep.app.audio.AudioManager.getInstance() }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf(0L) }
    var isCalculatingCache by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val updateState by updateViewModel.updateState.collectAsState()
    // 获取当前版本号
    val currentVersion = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo?.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    // 实时音量状态（用于显示当前音量）
    var currentVolumeDisplay by remember { mutableStateOf(0f) }
    
    // 定期更新缓存大小和音量显示
    LaunchedEffect(Unit) {
        while (true) {
            isCalculatingCache = true
            cacheSize = calculateCacheSize(context)
            isCalculatingCache = false
            
            // 更新音量显示：获取第一个正在播放的声音的音量，优先检查本地音频，然后检查远程音频
            val playingSounds = audioManager.getPlayingSounds()
            val playingRemoteSounds = audioManager.getPlayingRemoteSoundIds()
            currentVolumeDisplay = when {
                playingSounds.isNotEmpty() -> {
                    audioManager.getVolume(playingSounds.first())
                }
                playingRemoteSounds.isNotEmpty() -> {
                    audioManager.getRemoteVolume(playingRemoteSounds.first())
                }
                else -> {
                    // 如果没有正在播放的音频，保持当前显示值不变（不重置为0）
                    currentVolumeDisplay
                }
            }
            
            // 检查缓存是否超过200M (200 * 1024 * 1024 字节)
            val thresholdBytes = 200L * 1024 * 1024
            if (cacheSize > thresholdBytes && !isClearingCache) {
                // 自动清理缓存
                isClearingCache = true
                scope.launch {
                    try {
                        clearApplicationCache(context)
                        cacheSize = 0L
                        Toast.makeText(context, context.getString(R.string.cache_auto_cleared), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.auto_clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    } finally {
                        isClearingCache = false
                        // 清理完成后重新计算缓存大小
                        cacheSize = calculateCacheSize(context)
                    }
                }
            }
            
            // 每5秒更新一次缓存大小和音量
            delay(5000)
        }
    }
    
    // 实时监听音量变化（更频繁的更新）
    LaunchedEffect(Unit) {
        while (true) {
            val playingSounds = audioManager.getPlayingSounds()
            val playingRemoteSounds = audioManager.getPlayingRemoteSoundIds()
            // 只在有音频播放时才更新显示，避免覆盖用户设置的值
            when {
                playingSounds.isNotEmpty() -> {
                    currentVolumeDisplay = audioManager.getVolume(playingSounds.first())
                }
                playingRemoteSounds.isNotEmpty() -> {
                    currentVolumeDisplay = audioManager.getRemoteVolume(playingRemoteSounds.first())
                }
                // 如果没有正在播放的音频，保持当前显示值不变
            }
            delay(300) // 每300ms更新一次音量显示
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
		// 固定标题 + GitHub 按钮
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 16.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				context.getString(R.string.settings),
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold,
			)
			// 参考首页深浅色切换按钮样式的圆形按钮
			Surface(
				onClick = {
					val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(Constants.GITHUB_URL))
					intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
					try {
						context.startActivity(intent)
					} catch (_: Exception) {
					}
				},
				shape = CircleShape,
				color = MaterialTheme.colorScheme.surfaceVariant,
				modifier = Modifier.size(32.dp)
			) {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center
				) {
					// 使用 GitHub 官方图标资源
					androidx.compose.foundation.Image(
						painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_github),
						contentDescription = "GitHub",
						colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
					)
				}
			}
		}
        
        // 可滚动内容区域
        val scrollState = rememberScrollState()
        
        // 监听滚动状态，触发浮动按钮收缩
        var previousScrollValue by remember { mutableStateOf(scrollState.value) }
        LaunchedEffect(scrollState.value) {
            if (scrollState.value != previousScrollValue) {
                onScrollDetected()
                previousScrollValue = scrollState.value
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 外观设置
        Text(
            context.getString(R.string.appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
            Card(
            onClick = onNavigateToTheme,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.theme_and_colors), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.appearance_mode_theme_color),
                        style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 系统设置
        Text(
            context.getString(R.string.system),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 语言切换
        Card(
            onClick = { showLanguageDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.language), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.language_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentLanguage.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 隐藏动画文件
        SwitchItem(
            checked = hideAnimation,
            onCheckedChange = onHideAnimationChange,
            title = context.getString(R.string.hide_animation),
            description = context.getString(R.string.hide_lottie_animation_in_sound_cards)
        )
        
        // 一键调整音量
            Card(
            onClick = { showVolumeDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.adjust_all_volume), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.unified_adjust_all_sound_volume),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${(currentVolumeDisplay * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
            Card(
            onClick = { showClearCacheDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.clear_cache), style = MaterialTheme.typography.titleMedium)
                    Text(
                            context.getString(R.string.clear_app_cache_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCalculatingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            formatBytes(cacheSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 其他
        Text(
            context.getString(R.string.other),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 软件更新
        Card(
            onClick = { showUpdateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.software_update), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.check_and_update_to_latest_version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 加入群聊
        Card(
            onClick = {
                // 打开Telegram群聊链接
                val telegramUrl = Constants.TELEGRAM_URL
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "无法打开链接，请检查是否安装了Telegram或浏览器", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_telegram),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Column {
                        Text(context.getString(R.string.join_group), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.join_group_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 关于 XMSLEEP
        Card(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(context.getString(R.string.about_xmsleep), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.view_app_info_version_copyright),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        }
        
        // 软件更新对话框
        if (showUpdateDialog) {
            // 如果状态是Installing，检查是否有已下载的文件，如果有则重置为Downloaded状态
            // 注意：resetInstallingStateIfFileExists方法已移除，如需可以重新实现
            // LaunchedEffect(showUpdateDialog, updateState) {
            //     if (showUpdateDialog && updateState is org.xmsleep.app.update.UpdateState.Installing) {
            //         delay(100) // 短暂延迟确保UpdateDialog已初始化
            //         updateViewModel.resetInstallingStateIfFileExists()
            //     }
            // }
            
            UpdateDialog(
                onDismiss = { showUpdateDialog = false },
                updateViewModel = updateViewModel,
                currentLanguage = currentLanguage
            )
        }
        
        // 关于对话框
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                currentLanguage = currentLanguage,
                context = context
            )
        }
    
        // 缓存清理对话框
        if (showClearCacheDialog) {
            ClearCacheDialog(
                onDismiss = { 
                    if (!isClearingCache) {
                        showClearCacheDialog = false
                    }
                },
                onConfirm = {
                    // 立即关闭对话框，避免重复显示
                    showClearCacheDialog = false
                    isClearingCache = true
                    scope.launch {
                        try {
                            clearApplicationCache(context)
                            cacheSize = 0L  // 清理后重置缓存大小
                            Toast.makeText(context, context.getString(R.string.cache_cleared_success), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.cache_clear_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                        } finally {
                            isClearingCache = false
                            // 清理完成后重新计算缓存大小
                            cacheSize = calculateCacheSize(context)
                        }
                    }
                },
                isClearing = isClearingCache
            )
        }
        
        // 一键调整音量对话框
        var volume by remember { 
            mutableStateOf(
                // 获取第一个正在播放的声音的音量作为默认值，优先检查本地音频，然后检查远程音频
                audioManager.getPlayingSounds().firstOrNull()?.let { 
                    audioManager.getVolume(it) 
                } ?: audioManager.getPlayingRemoteSoundIds().firstOrNull()?.let {
                    audioManager.getRemoteVolume(it)
                } ?: 0.5f
            )
        }
        
        if (showVolumeDialog) {
            AlertDialog(
                onDismissRequest = { showVolumeDialog = false },
                title = { Text(context.getString(R.string.adjust_all_volume)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            context.getString(R.string.apply_to_all_playing_sounds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 音量滑块
                        Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = volume,
                            onValueChange = { 
                                volume = it
                                // 实时应用到所有本地声音
                                val localSounds = audioManager.getPlayingSounds()
                                localSounds.forEach { sound ->
                                    audioManager.setVolume(sound, volume)
                                }
                                // 实时应用到所有远程声音（繁星页面）
                                val remoteSoundIds = audioManager.getPlayingRemoteSoundIds()
                                android.util.Log.d("VolumeDialog", "正在播放的远程音频数量: ${remoteSoundIds.size}, IDs: $remoteSoundIds")
                                remoteSoundIds.forEach { soundId ->
                                    android.util.Log.d("VolumeDialog", "设置远程音频音量: $soundId = $volume")
                                    audioManager.setRemoteVolume(soundId, volume)
                                }
                                // 实时更新显示的音量数值
                                currentVolumeDisplay = volume
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
                        // 应用到所有本地声音（包括未播放的，以便下次播放时使用）
                        listOf(
                            org.xmsleep.app.audio.AudioManager.Sound.UMBRELLA_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.ROWING,
                            org.xmsleep.app.audio.AudioManager.Sound.OFFICE,
                            org.xmsleep.app.audio.AudioManager.Sound.LIBRARY,
                            org.xmsleep.app.audio.AudioManager.Sound.HEAVY_RAIN,
                            org.xmsleep.app.audio.AudioManager.Sound.TYPEWRITER,
                            org.xmsleep.app.audio.AudioManager.Sound.THUNDER_NEW,
                            org.xmsleep.app.audio.AudioManager.Sound.CLOCK,
                            org.xmsleep.app.audio.AudioManager.Sound.FOREST_BIRDS,
                            org.xmsleep.app.audio.AudioManager.Sound.DRIFTING,
                            org.xmsleep.app.audio.AudioManager.Sound.CAMPFIRE_NEW,
                            org.xmsleep.app.audio.AudioManager.Sound.WIND,
                            org.xmsleep.app.audio.AudioManager.Sound.KEYBOARD,
                            org.xmsleep.app.audio.AudioManager.Sound.SNOW_WALKING
                        ).forEach { sound ->
                            audioManager.setVolume(sound, volume)
                        }
                        // 应用到所有正在播放的远程声音（繁星页面）
                        val remoteSoundIds = audioManager.getPlayingRemoteSoundIds()
                        remoteSoundIds.forEach { soundId ->
                            audioManager.setRemoteVolume(soundId, volume)
                        }
                        // 更新显示的音量数值
                        currentVolumeDisplay = volume
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
        
        // 语言选择弹窗
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onLanguageSelected = { language: LanguageManager.Language ->
                    LanguageManager.setLanguage(context, language)
                    onLanguageChange(language) // 更新语言状态，触发实时切换
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

