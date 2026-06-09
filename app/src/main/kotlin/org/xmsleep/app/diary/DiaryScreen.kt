package org.xmsleep.app.diary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioResourceManager
import org.xmsleep.app.audio.model.SoundMetadata
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    onScrollDetected: () -> Unit = {}
) {
    val context = LocalContext.current
    val diaryManager = ListeningDiaryManager
    var history by remember { mutableStateOf(diaryManager.getHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    var remoteSoundsCache by remember { mutableStateOf<Map<String, SoundMetadata>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val resourceManager = AudioResourceManager.getInstance(context)
        val sounds = withContext(Dispatchers.IO) {
            resourceManager.getRemoteSounds()
        }
        remoteSoundsCache = sounds.associateBy { it.id }
    }

    val weeklySummary = remember(remoteSoundsCache) {
        diaryManager.generateWeeklySummaryRaw(context, remoteSoundsCache)
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            onScrollDetected()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        context.getString(R.string.diary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(x = (-4).dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.clear_history))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    org.xmsleep.app.ui.EmptyStateAnimation(animationSize = 240.dp)
                    Text(
                        text = context.getString(R.string.diary_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = context.getString(R.string.diary_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top)
                    )
                    .padding(paddingValues),
                state = scrollState,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (weeklySummary != null) {
                    item {
                        WeeklySummaryCard(summary = weeklySummary, context = context)
                    }
                }

                items(history) { entry ->
                    DiaryEntryCard(entry = entry, context = context, remoteSoundsCache = remoteSoundsCache)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(context.getString(R.string.clear_history)) },
            text = { Text(context.getString(R.string.diary_clear_history_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        diaryManager.clearHistory(context)
                        history = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text(context.getString(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun WeeklySummaryCard(summary: WeeklySummaryData, context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatChip(
                    label = summary.totalSessions.toString(),
                    sublabel = context.getString(R.string.diary_stat_sessions)
                )
                StatChip(
                    label = if (summary.totalMinutes >= 60) {
                        context.getString(R.string.diary_duration_hours, summary.totalMinutes / 60, summary.totalMinutes % 60)
                    } else {
                        context.getString(R.string.diary_duration_minutes, summary.totalMinutes)
                    },
                    sublabel = context.getString(R.string.diary_stat_duration)
                )
                if (summary.topSound != null) {
                    StatChip(
                        label = summary.topSound,
                        sublabel = context.getString(R.string.diary_stat_favorite)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, sublabel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiaryEntryCard(entry: DiaryEntry, context: android.content.Context, remoteSoundsCache: Map<String, SoundMetadata>) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val periodIcon = remember(entry.period) {
        when (entry.period) {
            "morning" -> Icons.Default.Brightness5
            "afternoon" -> Icons.Default.Brightness4
            "evening" -> Icons.Default.DarkMode
            else -> Icons.Default.GraphicEq
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = periodIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (entry.durationMinutes > 0) {
                    Text(
                        text = if (entry.durationMinutes >= 60) {
                            "${entry.durationMinutes / 60}h${entry.durationMinutes % 60}m"
                        } else {
                            "${entry.durationMinutes}min"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (entry.sounds.isNotEmpty()) {
                Text(
                    text = entry.sounds.joinToString(" · ") {
                        ListeningDiaryManager.getDisplaySoundName(context, it, remoteSoundsCache)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
