package org.xmsleep.app.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioImportManager
import org.xmsleep.app.audio.ImportCallback
import org.xmsleep.app.audio.ImportError
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.permissions.AudioPermissionManager
import org.xmsleep.app.permissions.PermissionCallback
import org.xmsleep.app.permissions.PermissionStatus

@Composable
fun ImportAudioButton(
    onImportSuccess: (SoundMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    
    val permissionManager = remember { AudioPermissionManager(context) }
    val importManager = remember { AudioImportManager(context) }
    
    // 权限回调
    val permissionCallback = object : PermissionCallback {
        override fun onPermissionGranted() {
            // 暂时显示开发中的消息
            errorMessage = "音频导入功能正在开发中"
            showErrorDialog = true
        }
        
        override fun onPermissionDenied(isPermanent: Boolean) {
            if (isPermanent) {
                errorMessage = "需要音频文件访问权限才能导入音频。请在系统设置中手动开启权限。"
            } else {
                errorMessage = "需要音频文件访问权限才能导入音频文件。"
            }
            showErrorDialog = true
        }
    }
    
    // 导入按钮点击处理
    fun handleImportClick() {
        if (activity == null) {
            errorMessage = "无法获取Activity上下文"
            showErrorDialog = true
            return
        }
        
        when (permissionManager.checkStoragePermission()) {
            PermissionStatus.GRANTED -> {
                // 直接显示错误，因为我们暂时无法使用文件选择器
                errorMessage = "音频导入功能正在开发中"
                showErrorDialog = true
            }
            PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> {
                showPermissionDialog = true
            }
            PermissionStatus.PERMANENTLY_DENIED -> {
                errorMessage = "音频文件访问权限已被永久拒绝。请在系统设置中手动开启权限。"
                showErrorDialog = true
            }
        }
    }
    
    // 导入按钮
    IconButton(
        onClick = { handleImportClick() },
        enabled = !isImporting,
        modifier = modifier
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "导入音频",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    // 权限请求对话框
    if (showPermissionDialog) {
        PermissionRequestDialog(
            onGrantPermission = {
                showPermissionDialog = false
                if (activity != null) {
                    permissionManager.requestStoragePermission(activity, permissionCallback)
                }
            },
            onDenyPermission = {
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
    
    // 错误对话框
    if (showErrorDialog) {
        ErrorDialog(
            message = errorMessage,
            onDismiss = {
                showErrorDialog = false
                errorMessage = ""
            },
            onOpenSettings = if (errorMessage.contains("系统设置")) {
                {
                    showErrorDialog = false
                    if (activity != null) {
                        permissionManager.openAppSettings(activity)
                    }
                }
            } else null
        )
    }
}

@Composable
fun PermissionRequestDialog(
    onGrantPermission: () -> Unit,
    onDenyPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "需要音频文件访问权限",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "为了让您能够导入和使用自己的音频文件，应用需要访问您设备上的音频文件。\n\n您的隐私很重要，我们只会访问您主动选择的音频文件。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDenyPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.not_now))
                    }
                    
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(context.getString(R.string.grant_permission_action))
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.import_failed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (onOpenSettings != null) {
                        Arrangement.spacedBy(12.dp)
                    } else {
                        Arrangement.Center
                    }
                ) {
                    if (onOpenSettings != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(context.getString(R.string.cancel))
                        }
                        
                        Button(
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(context.getString(R.string.open_settings))
                        }
                    } else {
                        Button(onClick = onDismiss) {
                            Text(context.getString(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}