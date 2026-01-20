package org.xmsleep.app.quote

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 每日一言对话框
 */
@Composable
fun DailyQuoteDialog(
    quote: Quote?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit = {},
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // 获取当前主题的颜色
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading || quote == null) {
                        // 加载状态
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在获取名句...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // 名句内容
                        // 日期 - 左对齐
                        Text(
                            text = LocalDate.now().format(
                                DateTimeFormatter.ofPattern("yyyy年MM月dd日 EEEE", Locale.CHINA)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 名句内容 - 放大 1.5 倍，左对齐
                        Text(
                            text = quote.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
                            textAlign = TextAlign.Start,
                            lineHeight = 36.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 作者和来源 - 右对齐，同一行，过长时自动换行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // 来源（如果有）
                            if (quote.from != null) {
                                Text(
                                    text = "《${quote.from}》",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // 作者
                            Text(
                                text = "— ${quote.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // 分享和保存按钮 - 上下排列，填充宽度
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 分享按钮
                            Button(
                                onClick = {
                                    if (isSharing) return@Button
                                    isSharing = true
                                    scope.launch {
                                        try {
                                            android.util.Log.d("DailyQuoteDialog", "开始分享流程")
                                            val bitmap = withContext(Dispatchers.Main) {
                                                ImageGenerator.generateQuoteImage(context, quote, isDarkTheme)
                                            }
                                            android.util.Log.d("DailyQuoteDialog", "图片生成成功，开始分享")
                                            ShareUtils.shareImage(context, bitmap, quote)
                                            android.util.Log.d("DailyQuoteDialog", "分享完成")
                                        } catch (e: Exception) {
                                            android.util.Log.e("DailyQuoteDialog", "分享失败", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isSharing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSharing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("分享中...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "分享",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("分享")
                                }
                            }
                            
                            // 保存图片按钮
                            OutlinedButton(
                                onClick = {
                                    if (isSaving) return@OutlinedButton
                                    
                                    // Android 9 及以下需要检查存储权限
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        
                                        if (!hasPermission) {
                                            Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }
                                    }
                                    
                                    isSaving = true
                                    scope.launch {
                                        try {
                                            android.util.Log.d("DailyQuoteDialog", "开始保存流程")
                                            val bitmap = withContext(Dispatchers.Main) {
                                                ImageGenerator.generateQuoteImage(context, quote, isDarkTheme)
                                            }
                                            android.util.Log.d("DailyQuoteDialog", "图片生成成功，开始保存")
                                            val result = ShareUtils.saveImageToGallery(context, bitmap)
                                            withContext(Dispatchers.Main) {
                                                if (result.isSuccess) {
                                                    Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "保存失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            android.util.Log.d("DailyQuoteDialog", "保存完成")
                                        } catch (e: Exception) {
                                            android.util.Log.e("DailyQuoteDialog", "保存失败", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("保存中...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "保存",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("保存图片")
                                }
                            }
                        }
                        
                        // 移除底部的加载指示器，因为已经集成到按钮中
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 刷新按钮 - 右下角角标样式（参考音频卡片下载角标）
                if (!isLoading && quote != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 0.dp,
                                    bottomEnd = 12.dp,
                                    bottomStart = 12.dp
                                )
                            )
                            .clickable(onClick = onRefresh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "换一句",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
