package org.xmsleep.app.quote

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 分享和保存工具类
 */
object ShareUtils {
    
    /**
     * 分享图片
     */
    suspend fun shareImage(context: Context, bitmap: Bitmap, quote: Quote) {
        withContext(Dispatchers.IO) {
            try {
                // 保存到缓存目录
                val cacheDir = File(context.cacheDir, "share")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                val file = File(cacheDir, "quote_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
                
                // 获取URI
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                // 创建分享文本
                val shareText = """
                    ${quote.text}
                    — ${quote.author}
                    
                    来自 XMSLEEP - 白噪音助眠应用
                    下载地址：https://github.com/Tosencen/XMSLEEP/releases
                """.trimIndent()
                
                // 分享
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "分享每日一言").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }
    
    /**
     * 保存图片到相册
     */
    suspend fun saveImageToGallery(context: Context, bitmap: Bitmap): Result<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.e("ShareUtils", "========== 开始保存图片到相册 ==========")
                val filename = "XMSLEEP_Quote_${System.currentTimeMillis()}.png"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 使用 MediaStore
                    android.util.Log.e("ShareUtils", "使用 MediaStore API (Android ${Build.VERSION.SDK_INT})")
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XMSLEEP")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    
                    if (uri == null) {
                        android.util.Log.e("ShareUtils", "创建 URI 失败")
                        return@withContext Result.failure(Exception("无法创建文件"))
                    }
                    
                    android.util.Log.e("ShareUtils", "URI 创建成功: $uri")
                    
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.flush()
                        android.util.Log.e("ShareUtils", "图片压缩${if (success) "成功" else "失败"}")
                    } ?: run {
                        android.util.Log.e("ShareUtils", "无法打开输出流")
                        return@withContext Result.failure(Exception("无法写入文件"))
                    }
                    
                    // 标记文件完成
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                    
                    android.util.Log.e("ShareUtils", "图片保存成功！")
                    Result.success(uri)
                } else {
                    // Android 9 及以下
                    android.util.Log.e("ShareUtils", "使用旧版 API (Android ${Build.VERSION.SDK_INT})")
                    @Suppress("DEPRECATION")
                    val imageUri = MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        bitmap,
                        filename,
                        "XMSLEEP 每日一言"
                    )
                    
                    if (imageUri != null) {
                        android.util.Log.e("ShareUtils", "图片保存成功: $imageUri")
                        Result.success(Uri.parse(imageUri))
                    } else {
                        android.util.Log.e("ShareUtils", "保存图片失败")
                        Result.failure(Exception("保存图片失败"))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ShareUtils", "保存图片异常: ${e.javaClass.simpleName}: ${e.message}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
