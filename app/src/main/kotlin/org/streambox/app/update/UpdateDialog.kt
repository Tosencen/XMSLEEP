package org.streambox.app.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 软件更新对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    updateViewModel: UpdateViewModel,
    context: Context
) {
    val updateState by updateViewModel.updateState.collectAsState()
    val downloadProgress by updateViewModel.downloadProgress.collectAsState()
    val downloadState by updateViewModel.downloadState.collectAsState()
    
    // 获取当前版本
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
    
    // 根据状态显示不同的内容
    when (val state = updateState) {
        is UpdateState.Idle -> {
            // 初始状态，检查更新
            LaunchedEffect(Unit) {
                updateViewModel.checkUpdate(currentVersion)
            }
            
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("软件更新") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("正在检查更新...")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("软件更新") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("正在检查更新...")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("软件更新") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "已是最新版本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "当前版本：$currentVersion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        
        is UpdateState.HasUpdate -> {
            val version = state.version
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("发现新版本") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    "版本 ${version.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "当前版本：$currentVersion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Divider()
                        
                        if (version.changelog.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "更新内容",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = parseMarkdown(version.changelog),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateViewModel.startDownload()
                        }
                    ) {
                        Text("立即更新")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            )
        }
        
        is UpdateState.CheckFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("检查更新失败") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(state.error)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateViewModel.checkUpdate(currentVersion)
                        }
                    ) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {
                    // 下载中不允许关闭
                },
                title = { Text("下载更新") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "已下载 ${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            updateViewModel.cancelDownload()
                            onDismiss()
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("下载完成") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("更新包已下载完成，是否立即安装？")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateViewModel.installApk(state.file)
                            onDismiss()
                        }
                    ) {
                        Text("立即安装")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("稍后")
                    }
                }
            )
        }
        
        is UpdateState.DownloadFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("下载失败") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(state.error)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val state = updateViewModel.updateState.value
                            if (state is UpdateState.HasUpdate) {
                                updateViewModel.startDownload()
                            }
                        }
                    ) {
                        Text("重试")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            )
        }
        
        is UpdateState.Installing -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("正在安装") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text("正在启动安装程序...")
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        
        is UpdateState.InstallFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("安装失败") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(state.error)
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 解析 Markdown 文本为 AnnotatedString，支持常见的 Markdown 格式
 */
private fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            
            when {
                // 分隔线
                line.trim().startsWith("---") -> {
                    append("\n")
                    i++
                    continue
                }
                
                // 一级标题
                line.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(line.removePrefix("# ").trim())
                    }
                    append("\n\n")
                    i++
                }
                
                // 二级标题
                line.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(line.removePrefix("## ").trim())
                    }
                    append("\n\n")
                    i++
                }
                
                // 三级标题
                line.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(line.removePrefix("### ").trim())
                    }
                    append("\n\n")
                    i++
                }
                
                // 无序列表
                line.trim().startsWith("- ") -> {
                    append("  • ")
                    appendMarkdownLine(line.removePrefix("- ").trim())
                    append("\n")
                    i++
                }
                
                // 空行
                line.isBlank() -> {
                    append("\n")
                    i++
                }
                
                // 普通文本
                else -> {
                    appendMarkdownLine(line)
                    append("\n")
                    i++
                }
            }
        }
    }
}

/**
 * 解析单行 Markdown，处理加粗等格式
 */
private fun AnnotatedString.Builder.appendMarkdownLine(text: String) {
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    var lastIndex = 0
    
    boldRegex.findAll(text).forEach { match ->
        // 添加加粗前的文本
        append(text.substring(lastIndex, match.range.first))
        
        // 添加加粗文本
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        
        lastIndex = match.range.last + 1
    }
    
    // 添加剩余的文本
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
