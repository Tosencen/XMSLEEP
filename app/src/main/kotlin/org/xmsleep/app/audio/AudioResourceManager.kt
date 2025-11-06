package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import android.util.Log
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
    
    // 音频清单缓存
    private var remoteManifest: SoundsManifest? = null
    
    /**
     * 加载网络音频清单
     */
    suspend fun loadRemoteManifest(): SoundsManifest? {
        return try {
            remoteManifest ?: run {
                remoteLoader.loadManifest().also { remoteManifest = it }
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
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

