package org.xmsleep.app.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager

/**
 * 缓存清理对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isClearing: Boolean
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        title = { Text(context.getString(R.string.clear_cache)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(context.getString(R.string.confirm_clear_cache))
                if (isClearing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            context.getString(R.string.clearing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        context.getString(R.string.this_will_clear_all_cache_data),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClearing
            ) {
                Text(context.getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearing
            ) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

/**
 * 关于对话框 - 显示应用信息、版本、版权和使用说明
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    currentLanguage: LanguageManager.Language,
    context: Context
) {
    val composeContext = LocalContext.current
    
    // 使用LaunchedEffect安全地获取应用版本信息，当语言变化时重新获取
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCodeInt by remember { mutableIntStateOf(1) }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo: android.content.pm.PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                composeContext.packageManager.getPackageInfo(
                    composeContext.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                composeContext.packageManager.getPackageInfo(composeContext.packageName, 0)
            }
            versionName = packageInfo?.versionName ?: "1.0.0"
            versionCodeInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toInt() ?: 1
            } else {
                @Suppress("DEPRECATION")
                (packageInfo?.versionCode ?: 1)
            }
        } catch (e: Exception) {
            android.util.Log.e("AboutDialog", "Error getting package info", e)
        }
    }
    
    // 使用key确保语言变化时重新组合
    key(currentLanguage) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { 
                Text(composeContext.getString(R.string.about_xmsleep), style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 应用名称和版本
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "XMSLEEP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            composeContext.getString(R.string.version, versionName, versionCodeInt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 应用说明（简化版）
                    Text(
                        composeContext.getString(R.string.app_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    HorizontalDivider()
                    
                    // 隐私政策
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            composeContext.getString(R.string.privacy_policy_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            composeContext.getString(R.string.privacy_policy),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 声音来源说明（简化版）
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            composeContext.getString(R.string.sound_source_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            composeContext.getString(R.string.sound_source),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // 版权信息
                    Text(
                        composeContext.getString(R.string.copyright),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(composeContext.getString(R.string.ok))
                }
            },
            dismissButton = null
        )
    }
}

/**
 * 语言选择弹窗
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: LanguageManager.Language,
    onLanguageSelected: (LanguageManager.Language) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(context.getString(R.string.select_language))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageManager.Language.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = language == currentLanguage,
                                onClick = { 
                                    if (language != currentLanguage) {
                                        onLanguageSelected(language)
                                    } else {
                                        onDismiss()
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.ok))
            }
        },
        dismissButton = null
    )
}
