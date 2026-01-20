package org.xmsleep.app.quote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 每日一言历史记录页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val quoteManager = remember { QuoteManager.getInstance(context) }
    var history by remember { mutableStateOf(quoteManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedQuote by remember { mutableStateOf<Quote?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.quote_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.clear_history))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            // 空状态 - 使用默认空状态动画
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
                    org.xmsleep.app.ui.EmptyStateAnimation(size = 240.dp)
                    Text(
                        text = context.getString(R.string.no_quote_history),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = context.getString(R.string.no_quote_history_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 历史记录列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    QuoteHistoryCard(
                        item = item,
                        onClick = { selectedQuote = item.quote }
                    )
                }
            }
        }
    }
    
    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(context.getString(R.string.clear_history)) },
            text = { Text(context.getString(R.string.clear_history_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        quoteManager.clearHistory()
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
    
    // 名句详情对话框
    if (selectedQuote != null) {
        DailyQuoteDialog(
            quote = selectedQuote!!,
            onDismiss = { selectedQuote = null }
        )
    }
}

/**
 * 历史记录卡片
 */
@Composable
fun QuoteHistoryCard(
    item: QuoteHistoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 日期时间
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 名句内容
            Text(
                text = item.quote.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // 作者和来源
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (item.quote.from != null) {
                    Text(
                        text = "《${item.quote.from}》",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "— ${item.quote.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
    return sdf.format(Date(timestamp))
}
