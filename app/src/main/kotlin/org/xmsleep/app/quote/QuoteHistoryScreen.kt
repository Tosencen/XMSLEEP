package org.xmsleep.app.quote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onBack: () -> Unit,
    onScrollDetected: () -> Unit = {}
) {
    val context = LocalContext.current
    val quoteManager = remember { QuoteManager.getInstance(context) }
    var history by remember { mutableStateOf(quoteManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedQuote by remember { mutableStateOf<Quote?>(null) }
    var dailyImageUrl by remember { mutableStateOf<String?>(null) }
    var dailyImageCopyright by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberLazyListState()
    
    // 监听滚动事件
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            onScrollDetected()
        }
    }

    // 打开历史名句时，拉取随机每日一图作为弹窗背景
    LaunchedEffect(selectedQuote) {
        if (selectedQuote != null) {
            dailyImageUrl = null
            dailyImageCopyright = null
            val img = DailyImageProvider.getTodayImage(random = true)
            dailyImageUrl = img?.url
            dailyImageCopyright = img?.copyright
        }
    }
    
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent, // 设置透明背景，让背景动画显示
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        context.getString(R.string.quote_history),
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
                    containerColor = androidx.compose.ui.graphics.Color.Transparent // TopBar 也设置透明
                ),
                windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
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
                    org.xmsleep.app.ui.EmptyStateAnimation(animationSize = 240.dp)
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
                    .consumeWindowInsets(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Top)
                    )
                    .padding(paddingValues),
                state = scrollState,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp),
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
            onDismiss = { selectedQuote = null },
            imageUrl = dailyImageUrl,
            imageCopyright = dailyImageCopyright
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
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 日期时间
            Text(
                text = formatTimestamp(item.timestamp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 名句内容
            Text(
                text = item.quote.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 作者和来源
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.quote.from != null) {
                    Text(
                        text = "《${item.quote.from}》",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "— ${item.quote.author}",
                    style = MaterialTheme.typography.bodyMedium,
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
