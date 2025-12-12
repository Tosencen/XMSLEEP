package org.xmsleep.app.audio

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 本地音频媒体服务
 * 处理本地音频文件的删除和重命名操作
 */
class LocalAudioMediaService private constructor(
    private val context: Context
) {
    private val contentResolver = context.contentResolver
    private var resultOkCallback: () -> Unit = {}
    private var resultCancelledCallback: () -> Unit = {}
    private var mediaRequestLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    
    /**
     * 在 Activity 中初始化服务
     * 必须在 Activity 的 onCreate 中调用
     */
    fun initialize(activity: ComponentActivity) {
        mediaRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> resultOkCallback()
                Activity.RESULT_CANCELED -> resultCancelledCallback()
            }
        }
    }
    
    /**
     * 删除媒体文件
     */
    suspend fun deleteMedia(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteMediaR(uri)
        } else {
            deleteMediaBelowR(uri)
        }
    }
    
    /**
     * 重命名媒体文件
     */
    suspend fun renameMedia(uri: Uri, newName: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            renameMediaR(uri, newName)
        } else {
            withContext(Dispatchers.IO) {
                renameMediaBelowR(uri, newName)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchWriteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {}
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createWriteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent.intentSender).build())
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchDeleteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {}
    ) {
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        MediaStore.createDeleteRequest(contentResolver, uris).also { intent ->
            mediaRequestLauncher?.launch(IntentSenderRequest.Builder(intent.intentSender).build())
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMediaR(uri: Uri): Boolean = suspendCancellableCoroutine { continuation ->
        launchDeleteRequest(
            uris = listOf(uri),
            onResultOk = { continuation.resume(true) },
            onResultCanceled = { continuation.resume(false) }
        )
    }
    
    private suspend fun deleteMediaBelowR(uri: Uri): Boolean {
        return try {
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun renameMediaR(uri: Uri, newName: String): Boolean = suspendCancellableCoroutine { continuation ->
        launchWriteRequest(
            uris = listOf(uri),
            onResultOk = {
                // 权限授予后，在 IO 线程中执行重命名
                CoroutineScope(Dispatchers.IO).launch {
                    val result = try {
                        val values = ContentValues().apply {
                            put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
                        }
                        val updateCount = contentResolver.update(uri, values, null, null)
                        android.util.Log.d("LocalAudioMediaService", "重命名更新结果: $updateCount")
                        updateCount > 0
                    } catch (e: Exception) {
                        android.util.Log.e("LocalAudioMediaService", "重命名失败", e)
                        false
                    }
                    continuation.resume(result)
                }
            },
            onResultCanceled = { 
                android.util.Log.d("LocalAudioMediaService", "用户取消重命名")
                continuation.resume(false) 
            }
        )
    }
    
    private suspend fun renameMediaBelowR(uri: Uri, newName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, newName)
            }
            contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            android.util.Log.e("LocalAudioMediaService", "重命名失败", e)
            false
        }
    }
    
    companion object {
        @Volatile
        private var instance: LocalAudioMediaService? = null
        
        fun getInstance(context: Context): LocalAudioMediaService {
            return instance ?: synchronized(this) {
                instance ?: LocalAudioMediaService(context.applicationContext).also { instance = it }
            }
        }
    }
}
