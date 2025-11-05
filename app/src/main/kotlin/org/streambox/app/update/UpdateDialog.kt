package org.streambox.app.update

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.streambox.app.R
import org.streambox.app.i18n.LanguageManager

/**
 * 软件更新对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    onDismiss: () -> Unit,
    updateViewModel: UpdateViewModel,
    currentLanguage: LanguageManager.Language
) {
    val context = LocalContext.current
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
    
    // 使用key确保语言切换时重新组合
    key(currentLanguage) {
        // 根据状态显示不同的内容
        when (val state = updateState) {
        is UpdateState.Idle -> {
            // 初始状态，检查更新
            LaunchedEffect(Unit) {
                updateViewModel.checkUpdate(currentVersion)
            }
            
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(context.getString(R.string.software_update)) },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text(context.getString(R.string.checking_update))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.cancel))
                        }
                    }
                )
            }
        
        is UpdateState.Checking -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.software_update)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(context.getString(R.string.checking_update))
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        is UpdateState.UpToDate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.software_update)) },
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
                                context.getString(R.string.up_to_date),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            context.getString(R.string.current_version, currentVersion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.ok))
                    }
                }
            )
        }
        
        is UpdateState.HasUpdate -> {
            val version = state.version
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.new_version_found)) },
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
                                    context.getString(R.string.version_name, version.version),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    context.getString(R.string.current_version, currentVersion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Divider()
                        
                        if (version.changelog.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    context.getString(R.string.update_content),
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
                        Text(context.getString(R.string.update_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.later))
                    }
                }
            )
        }
        
        is UpdateState.CheckFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.check_update_failed)) },
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
                        Text(context.getString(R.string.retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        is UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {
                    // 下载中不允许关闭
                },
                title = { Text(context.getString(R.string.downloading_update)) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            context.getString(R.string.downloaded_percent, (state.progress * 100).toInt()),
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
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        is UpdateState.Downloaded -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.download_complete)) },
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
                        Text(context.getString(R.string.download_complete_message))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateViewModel.installApk(state.file)
                            onDismiss()
                        }
                    ) {
                        Text(context.getString(R.string.install_now))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.later))
                    }
                }
            )
        }
        
        is UpdateState.DownloadFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.download_failed)) },
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
                        Text(context.getString(R.string.retry))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.cancel))
                    }
                }
            )
        }
        
        is UpdateState.Installing -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.installing)) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(context.getString(R.string.starting_installer))
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(context.getString(R.string.ok))
                    }
                }
            )
        }
        
        is UpdateState.InstallFailed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(context.getString(R.string.install_failed)) },
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
                        Text(context.getString(R.string.ok))
                    }
                }
            )
        }
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
