package org.xmsleep.app.ui.starsky

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.xmsleep.app.R
import org.xmsleep.app.ui.components.FrostedPopup
import org.xmsleep.app.ui.components.FrostedGlassSheet
import org.xmsleep.app.audio.AudioCacheManager
import org.xmsleep.app.audio.DownloadProgress
import org.xmsleep.app.audio.model.AudioSource
import org.xmsleep.app.audio.model.SoundCategory
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.i18n.LanguageManager
import java.util.Locale

@Composable
fun StorageManagementDialog(
    remoteSounds: List<SoundMetadata>,
    remoteCategories: List<SoundCategory>,
    cacheManager: AudioCacheManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LanguageManager.getCurrentLanguage(context)
    val scope = rememberCoroutineScope()

    val soundsByCategory = remember(remoteSounds) {
        remoteSounds.groupBy { it.category }
    }

    val downloadableCategoryIds = remember(remoteSounds, remoteCategories) {
        val idsFromSounds = remoteSounds.map { it.category }.distinct().toSet()
        val sorted = if (remoteCategories.isNotEmpty()) {
            remoteCategories
                .filter { it.id in idsFromSounds }
                .sortedBy { it.order }
                .map { it.id }
        } else {
            idsFromSounds.sorted()
        }
        sorted.filter { categoryId ->
            soundsByCategory[categoryId]?.any {
                it.source == AudioSource.REMOTE && it.remoteUrl != null
            } == true
        }
    }

    val categoryDisplayName: (String) -> String = { categoryId ->
        val category = remoteCategories.find { it.id == categoryId }
        category?.getLocalizedName(currentLanguage) ?: categoryId
    }

    fun getCategoryTotalCount(categoryId: String): Int {
        return soundsByCategory[categoryId]?.count {
            it.source == AudioSource.REMOTE && it.remoteUrl != null
        } ?: 0
    }

    var cacheSize by remember { mutableStateOf(cacheManager.getCacheSize()) }
    var categoryCachedCounts by remember {
        mutableStateOf(
            downloadableCategoryIds.associateWith { categoryId ->
                soundsByCategory[categoryId]?.count {
                    it.source == AudioSource.REMOTE && it.remoteUrl != null
                            && cacheManager.getCachedFile(it.id) != null
                } ?: 0
            }
        )
    }

    fun refreshStats() {
        cacheSize = cacheManager.getCacheSize()
        categoryCachedCounts = downloadableCategoryIds.associateWith { categoryId ->
            soundsByCategory[categoryId]?.count {
                it.source == AudioSource.REMOTE && it.remoteUrl != null
                        && cacheManager.getCachedFile(it.id) != null
            } ?: 0
        }
    }

    var downloadingCategoryId by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var categoryDownloadProgress by remember { mutableStateOf(0f) }
    var categoryCompletedCount by remember { mutableStateOf(0) }
    var categoryTotalCount by remember { mutableStateOf(0) }
    var categoryCurrentSoundName by remember { mutableStateOf("") }

    fun startCategoryDownload(categoryId: String) {
        val soundsToDownload = soundsByCategory[categoryId]?.filter {
            it.source == AudioSource.REMOTE && it.remoteUrl != null
                    && cacheManager.getCachedFile(it.id) == null
        } ?: return
        if (soundsToDownload.isEmpty()) return

        downloadingCategoryId = categoryId
        categoryDownloadProgress = 0f
        categoryCompletedCount = 0
        categoryTotalCount = soundsToDownload.size
        categoryCurrentSoundName = ""

        downloadJob = scope.launch {
            soundsToDownload.forEachIndexed { index, sound ->
                categoryCurrentSoundName = sound.getLocalizedName(currentLanguage)

                cacheManager.downloadAudioWithProgress(sound.remoteUrl!!, sound.id)
                    .collect { progress ->
                        when (progress) {
                            is DownloadProgress.Progress -> {
                                val fileProgress = if (progress.contentLength > 0)
                                    progress.bytesRead.toFloat() / progress.contentLength.toFloat() else 0f
                                categoryDownloadProgress = (index + fileProgress) / categoryTotalCount
                            }
                            is DownloadProgress.Success -> {
                                categoryCompletedCount++
                                categoryDownloadProgress = categoryCompletedCount.toFloat() / categoryTotalCount
                                refreshStats()
                            }
                            is DownloadProgress.Error -> {
                                categoryCompletedCount++
                                categoryDownloadProgress = categoryCompletedCount.toFloat() / categoryTotalCount
                            }
                        }
                    }
            }
            val catName = categoryDisplayName(categoryId)
            val successCount = categoryCompletedCount
            val totalCount = categoryTotalCount
            downloadingCategoryId = null
            refreshStats()
            Toast.makeText(context, context.getString(R.string.storage_download_complete, catName, successCount, totalCount), Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadingCategoryId = null
        refreshStats()
    }

    FrostedPopup(visible = true) {
        FrostedGlassSheet(
            visible = true,
            onDismissRequest = {
                downloadJob?.cancel()
                onDismiss()
            }
        ) {
            Text(
                context.getString(R.string.storage_management),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(context.getString(R.string.storage_space, formatBytes(cacheSize)))

                Spacer(Modifier.height(12.dp))

                downloadableCategoryIds.forEach { categoryId ->
                    val totalCount = getCategoryTotalCount(categoryId)
                    val cachedCount = categoryCachedCounts[categoryId] ?: 0
                    val isFullyCached = cachedCount >= totalCount
                    val isThisDownloading = downloadingCategoryId == categoryId

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = when {
                                isFullyCached -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                isThisDownloading -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AnimatedVisibility(
                                        visible = isFullyCached,
                                        enter = scaleIn() + fadeIn(),
                                        exit = scaleOut() + fadeOut()
                                    ) {
                                        Icon(
                                            imageVector = BookmarkCheckFilled,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = categoryDisplayName(categoryId),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isThisDownloading && categoryCurrentSoundName.isNotEmpty())
                                                    "$categoryCompletedCount/$categoryTotalCount · $categoryCurrentSoundName"
                                                else
                                                    "$cachedCount/$totalCount",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            when {
                                                isThisDownloading -> {
                                                    TextButton(onClick = { cancelDownload() }) {
                                                        Text(context.getString(R.string.cancel), color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                                !isFullyCached -> {
                                                    OutlinedButton(
                                                        onClick = { startCategoryDownload(categoryId) },
                                                        enabled = downloadingCategoryId == null,
                                                        border = BorderStroke(
                                                            width = 1.dp,
                                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Download,
                                                            contentDescription = "下载",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isThisDownloading) {
                                    LinearProgressIndicator(
                                        progress = { categoryDownloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = {
                    downloadJob?.cancel()
                    onDismiss()
                }) {
                    Text(context.getString(R.string.close))
                }
                TextButton(
                    onClick = {
                        cacheManager.clearCache()
                        refreshStats()
                        Toast.makeText(context, context.getString(R.string.clear_downloaded), Toast.LENGTH_SHORT).show()
                    },
                    enabled = cacheSize > 0 && downloadingCategoryId == null
                ) {
                    Text(context.getString(R.string.clear_downloaded))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.2f KB", kb)
        else -> "$bytes B"
    }
}
