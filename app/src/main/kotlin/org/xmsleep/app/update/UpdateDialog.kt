package org.xmsleep.app.update

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
import org.xmsleep.app.R
import org.xmsleep.app.ui.components.FrostedPopup
import org.xmsleep.app.ui.components.FrostedGlassSheet
import org.xmsleep.app.i18n.LanguageManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val updateState by updateViewModel.updateState.collectAsStateWithLifecycle()
    val downloadProgress by updateViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()
    
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
            
            UpdateProgressDialog(onDismiss = onDismiss)
            }
        
        is UpdateState.Checking -> {
            UpdateProgressDialog(onDismiss = onDismiss)
        }
        
        is UpdateState.UpToDate -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.software_update),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.ok))
                        }
                    }
                }
            }
        }
        
        is UpdateState.HasUpdate -> {
            val version = state.version
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.new_version_found),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
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
                        
                        HorizontalDivider()
                        
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.later))
                        }
                        Button(
                            onClick = {
                                updateViewModel.startDownload()
                            }
                        ) {
                            Text(context.getString(R.string.update_now))
                        }
                    }
                }
            }
        }
        
        is UpdateState.CheckFailed -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.check_update_failed),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            context.getString(R.string.check_update_failed_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.cancel))
                        }
                        TextButton(
                            onClick = {
                                updateViewModel.reset()
                                updateViewModel.checkUpdate(currentVersion)
                            }
                        ) {
                            Text(context.getString(R.string.retry))
                        }
                    }
                }
            }
        }
        
        is UpdateState.Downloading -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = {
                        // 下载中不允许关闭
                    }
                ) {
                    Text(
                        context.getString(R.string.downloading_update),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = {
                                updateViewModel.cancelDownload()
                                onDismiss()
                            }
                        ) {
                            Text(context.getString(R.string.cancel))
                        }
                    }
                }
            }
        }
        
        is UpdateState.Downloaded -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.download_complete),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.later))
                        }
                        Button(
                            onClick = {
                                updateViewModel.installApk(state.file)
                                // 不立即关闭对话框，等待权限请求结果
                            }
                        ) {
                            Text(context.getString(R.string.install_now))
                        }
                    }
                }
            }
        }
        
        is UpdateState.InstallPermissionRequested -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.install_permission_required),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(context.getString(R.string.install_permission_hint))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                // 用户从设置返回后，重试安装
                                updateViewModel.retryInstall()
                            }
                        ) {
                            Text(context.getString(R.string.allowed_continue_install))
                        }
                    }
                }
            }
        }
        
        is UpdateState.DownloadFailed -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.download_failed),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            state.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            context.getString(R.string.download_failed_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.cancel))
                        }
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
                    }
                }
            }
        }
        
        is UpdateState.Installing -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.installing),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(context.getString(R.string.starting_installer))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.ok))
                        }
                    }
                }
            }
        }
        
        is UpdateState.InstallFailed -> {
            FrostedPopup(visible = true) {
                FrostedGlassSheet(
                    visible = true,
                    onDismissRequest = onDismiss
                ) {
                    Text(
                        context.getString(R.string.install_failed),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(state.error)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(context.getString(R.string.ok))
                        }
                    }
                }
            }
        }
        }
    }
}

/**
 * 软件更新"检查中"进度对话框。
 * UpdateState.Idle 与 UpdateState.Checking 两个状态显示内容完全一致，
 * 抽成共用组件避免重复（注意：发起检查的 LaunchedEffect 只应存在于 Idle 分支）。
 */
@Composable
private fun UpdateProgressDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    FrostedPopup(visible = true) {
        FrostedGlassSheet(
            visible = true,
            onDismissRequest = onDismiss
        ) {
            Text(
                context.getString(R.string.software_update),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(context.getString(R.string.checking_update))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }
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
