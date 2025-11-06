package org.xmsleep.app.audio

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmsleep.app.audio.model.SoundsManifest
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 网络音频加载器
 * 负责从GitHub加载音频清单和音频文件
 */
class RemoteAudioLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "RemoteAudioLoader"
        // GitHub raw URL（需要替换为实际的仓库地址）
        private const val REMOTE_MANIFEST_URL = 
            "https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/sounds_remote.json"
    }
    
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * 加载网络音频清单
     */
    suspend fun loadManifest(forceRefresh: Boolean = false): SoundsManifest {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(REMOTE_MANIFEST_URL)
                    .apply {
                        if (forceRefresh) {
                            addHeader("Cache-Control", "no-cache")
                        }
                    }
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("加载清单失败: ${response.code}")
                }
                
                val json = response.body?.string() 
                    ?: throw IOException("响应体为空")
                
                val manifest = gson.fromJson(json, SoundsManifest::class.java)
                
                Log.d(TAG, "成功加载网络音频清单，共 ${manifest.sounds.size} 个音频")
                manifest
            } catch (e: Exception) {
                Log.e(TAG, "加载网络音频清单失败: ${e.message}")
                throw e
            }
        }
    }
}

