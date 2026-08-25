package org.xmsleep.app.quote

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.R
import org.xmsleep.app.utils.Logger
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
    isLoading: Boolean = false,
    imageUrl: String? = null,
    imageCopyright: String? = null
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // 每日一图背景（随机每张不写磁盘缓存）
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1C1B1F))
                    )
                }

                // 深色渐变 scrim，保证白色文字可读
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.25f),
                                    1f to Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
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
                                text = context.getString(R.string.loading_quote),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
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
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 名句内容 - 放大 1.5 倍，左对齐
                        Text(
                            text = quote.text,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
                            color = Color.White,
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
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // 作者
                            Text(
                                text = "— ${quote.author}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
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
                                            Logger.d("DailyQuoteDialog", "开始分享流程")
                                            val bitmap = withContext(Dispatchers.Main) {
                                                ImageGenerator.generateQuoteImage(context, quote, isDarkTheme, imageUrl = imageUrl)
                                            }
                                            Logger.d("DailyQuoteDialog", "图片生成成功，开始分享")
                                            ShareUtils.shareImage(context, bitmap, quote)
                                            Logger.d("DailyQuoteDialog", "分享完成")
                                        } catch (e: Exception) {
                                            Logger.e("DailyQuoteDialog", "分享失败", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.share_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                                    Text(context.getString(R.string.sharing))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = context.getString(R.string.share),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.share))
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
                                            Toast.makeText(context, context.getString(R.string.storage_permission_required_for_save), Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }
                                    }
                                    
                                    isSaving = true
                                    scope.launch {
                                        try {
                                            Logger.d("DailyQuoteDialog", "开始保存流程")
                                            val bitmap = withContext(Dispatchers.Main) {
                                                ImageGenerator.generateQuoteImage(context, quote, isDarkTheme, imageUrl = imageUrl)
                                            }
                                            Logger.d("DailyQuoteDialog", "图片生成成功，开始保存")
                                            val result = ShareUtils.saveImageToGallery(context, bitmap)
                                            withContext(Dispatchers.Main) {
                                                if (result.isSuccess) {
                                                    Toast.makeText(context, context.getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, context.getString(R.string.save_failed, result.exceptionOrNull()?.message ?: ""), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            Logger.d("DailyQuoteDialog", "保存完成")
                                        } catch (e: Exception) {
                                            Logger.e("DailyQuoteDialog", "保存失败", e)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.save_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
                                    Text(context.getString(R.string.saving))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = context.getString(R.string.save),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(context.getString(R.string.save_image))
                                }
                            }
                        }
                        
                        // 移除底部的加载指示器，因为已经集成到按钮中
                        if (imageCopyright != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = imageCopyright,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // 刷新按钮 - 右下角角标样式（参考音频卡片下载角标）
                if (!isLoading && quote != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 16.dp,
                                    bottomEnd = 0.dp,
                                    bottomStart = 16.dp
                                )
                            )
                            .clickable(onClick = onRefresh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = context.getString(R.string.refresh_quote),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
