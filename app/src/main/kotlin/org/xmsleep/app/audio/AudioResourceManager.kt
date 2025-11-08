package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmsleep.app.audio.model.AudioSource
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.audio.model.SoundsManifest
import java.io.File

/**
 * 音频资源管理器
 * 负责管理所有音频资源（本地和网络）
 */
class AudioResourceManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "AudioResourceManager"
        
        @Volatile
        private var instance: AudioResourceManager? = null
        
        fun getInstance(context: Context): AudioResourceManager {
            return instance ?: synchronized(this) {
                instance ?: AudioResourceManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    private val remoteLoader = RemoteAudioLoader(context)
    private val cacheManager = AudioCacheManager.getInstance(context)
    private val gson = Gson()
    
    // 音频清单缓存
    private var remoteManifest: SoundsManifest? = null
    
    // 持久化缓存文件名
    private val manifestCacheFile: File by lazy {
        File(appContext.cacheDir, "remote_manifest_cache.json")
    }
    
    /**
     * 获取当前缓存的清单（同步，快速）
     * 优先返回内存缓存，如果没有则返回持久化缓存
     */
    fun getCachedManifest(): SoundsManifest? {
        return remoteManifest ?: loadPersistedManifest()
    }
    
    /**
     * 加载网络音频清单（优先从内存缓存，然后从持久化缓存，最后从网络）
     */
    suspend fun loadRemoteManifest(): SoundsManifest? {
        return try {
            // 先检查内存缓存
            if (remoteManifest != null) {
                return remoteManifest
            }
            
            // 再检查持久化缓存
            val persistedManifest = loadPersistedManifest()
            if (persistedManifest != null) {
                return persistedManifest
            }
            
            // 最后从网络加载
            remoteLoader.loadManifest().also { manifest ->
                remoteManifest = manifest
                // 保存到持久化缓存
                savePersistedManifest(manifest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载网络音频清单失败: ${e.message}")
            null
        }
    }
    
    /**
     * 获取所有网络音频
     */
    suspend fun getRemoteSounds(): List<SoundMetadata> {
        return loadRemoteManifest()?.sounds ?: emptyList()
    }
    
    /**
     * 根据ID获取音频元数据
     */
    suspend fun getSoundMetadata(soundId: String): SoundMetadata? {
        // 查找网络资源
        return loadRemoteManifest()?.sounds?.find { it.id == soundId }
    }
    
    /**
     * 获取音频文件URI（用于播放）
     */
    suspend fun getSoundUri(metadata: SoundMetadata): Uri? {
        return when (metadata.source) {
            AudioSource.LOCAL -> {
                metadata.localResourceId?.let { resourceId ->
                    Uri.parse("android.resource://${appContext.packageName}/$resourceId")
                }
            }
            AudioSource.REMOTE -> {
                // 先检查缓存
                cacheManager.getCachedFile(metadata.id)?.let { file ->
                    Uri.fromFile(file)
                } ?: run {
                    // 如果未缓存，返回网络URL（ExoPlayer支持流式播放）
                    metadata.remoteUrl?.let { Uri.parse(it) }
                }
            }
        }
    }
    
    /**
     * 确保音频已下载（网络资源）
     */
    suspend fun ensureSoundDownloaded(metadata: SoundMetadata): Result<File> {
        if (metadata.source != AudioSource.REMOTE) {
            return Result.failure(IllegalArgumentException("不是网络资源"))
        }
        
        val remoteUrl = metadata.remoteUrl ?: return Result.failure(
            IllegalArgumentException("网络URL为空")
        )
        
        // 检查缓存
        cacheManager.getCachedFile(metadata.id)?.let { file ->
            if (file.exists()) {
                return Result.success(file)
            }
        }
        
        // 下载音频
        return cacheManager.downloadAudio(remoteUrl, metadata.id)
    }
    
    /**
     * 刷新网络音频清单
     */
    suspend fun refreshRemoteManifest(): Result<SoundsManifest> {
        return try {
            val manifest = remoteLoader.loadManifest(forceRefresh = true)
            remoteManifest = manifest
            // 保存到持久化缓存
            savePersistedManifest(manifest)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从持久化缓存加载清单（同步，快速）
     */
    fun loadPersistedManifest(): SoundsManifest? {
        return try {
            if (manifestCacheFile.exists() && manifestCacheFile.length() > 0) {
                val json = manifestCacheFile.readText()
                val manifest = gson.fromJson(json, SoundsManifest::class.java)
                // 同时更新内存缓存
                remoteManifest = manifest
                Log.d(TAG, "从持久化缓存加载清单成功，共 ${manifest.sounds.size} 个音频")
                manifest
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "从持久化缓存加载清单失败: ${e.message}")
            null
        }
    }
    
    /**
     * 保存清单到持久化缓存（异步）
     */
    private suspend fun savePersistedManifest(manifest: SoundsManifest) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(manifest)
                manifestCacheFile.writeText(json)
                Log.d(TAG, "清单已保存到持久化缓存")
            } catch (e: Exception) {
                Log.e(TAG, "保存清单到持久化缓存失败: ${e.message}")
            }
        }
    }
}

