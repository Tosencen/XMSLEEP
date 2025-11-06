package org.xmsleep.app.update

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 更新状态管理 ViewModel
 */
class UpdateViewModel(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 从 BuildConfig 读取 GitHub Token（如果配置了）
    private val githubToken: String? = try {
        val buildConfigClass = Class.forName("org.xmsleep.app.BuildConfig")
        val tokenField = buildConfigClass.getField("GITHUB_TOKEN")
        val token = tokenField.get(null) as? String
        if (token.isNullOrBlank()) null else token
    } catch (e: Exception) {
        android.util.Log.d("UpdateCheck", "无法读取GITHUB_TOKEN，使用未认证请求")
        null
    }
    
    private val updateChecker = UpdateChecker(githubToken = githubToken)
    private val fileDownloader = FileDownloader()
    private val updateInstaller = UpdateInstaller(context)
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    val downloadProgress = fileDownloader.progress
    val downloadState = fileDownloader.state
    
    private var _latestVersion: NewVersion? = null
    val latestVersion: NewVersion?
        get() = _latestVersion
    
    private var lastCheckTime: Long = 0L
    
    /**
     * 自动检查更新（带时间间隔限制，1小时内只检查一次）
     */
    fun startAutomaticCheckLatestVersion(currentVersion: String) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCheck = currentTime - lastCheckTime
        // 1小时内只检查一次
        if (lastCheckTime > 0 && timeSinceLastCheck < 1000 * 60 * 60 * 1) {
            android.util.Log.d("UpdateCheck", "距离上次检查仅 ${timeSinceLastCheck / 1000 / 60} 分钟，跳过本次检查")
            return
        }
        android.util.Log.d("UpdateCheck", "开始检查更新，当前版本: $currentVersion")
        checkUpdate(currentVersion)
    }
    
    /**
     * 检查更新
     * @param currentVersion 当前版本号
     */
    fun checkUpdate(currentVersion: String) {
        scope.launch {
            _updateState.value = UpdateState.Checking
            lastCheckTime = System.currentTimeMillis()
            android.util.Log.d("UpdateCheck", "开始调用UpdateChecker.checkLatestVersion，当前版本: $currentVersion")
            try {
                val newVersion = withContext(Dispatchers.IO) {
                    updateChecker.checkLatestVersion(currentVersion)
                }
                
                android.util.Log.d("UpdateCheck", "UpdateChecker返回结果: ${if (newVersion != null) "有新版本 ${newVersion.version}" else "无新版本"}")
                
                if (newVersion != null) {
                    _latestVersion = newVersion
                    _updateState.value = UpdateState.HasUpdate(newVersion)
                    android.util.Log.d("UpdateCheck", "已设置更新状态为HasUpdate")
                } else {
                    _updateState.value = UpdateState.UpToDate
                    android.util.Log.d("UpdateCheck", "已设置更新状态为UpToDate")
                }
            } catch (e: IOException) {
                // 网络错误或 rate limit 错误
                val errorMsg = e.message ?: "网络连接失败，请检查网络后重试"
                android.util.Log.e("UpdateCheck", "检查更新失败 (IOException): $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "检查更新失败"
                android.util.Log.e("UpdateCheck", "检查更新失败 (Exception): $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            }
        }
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload() {
        val version = _latestVersion ?: return
        
        scope.launch {
            _updateState.value = UpdateState.Downloading(0f)
            
            // 获取下载目录
            val downloadDir = File(context.getExternalFilesDir(null), "updates")
            downloadDir.mkdirs()
            
            val apkFile = File(downloadDir, "XMSLEEP-${version.version}.apk")
            
            // 监听下载进度和状态
            var progressJob: Job? = null
            var stateJob: Job? = null
            
            progressJob = scope.launch {
                downloadProgress.collect { progress ->
                    _updateState.value = UpdateState.Downloading(progress)
                }
            }
            
            stateJob = scope.launch {
                downloadState.collect { state ->
                    when (state) {
                        is DownloadState.Success -> {
                            _updateState.value = UpdateState.Downloaded(state.file)
                            progressJob?.cancel()
                        }
                        is DownloadState.Failed -> {
                            _updateState.value = UpdateState.DownloadFailed(state.error)
                            progressJob?.cancel()
                        }
                        is DownloadState.Downloading -> {
                            // 由 progress 处理
                        }
                        is DownloadState.Idle -> {
                            // 忽略
                        }
                    }
                }
            }
            
            // 下载文件
            val downloadedFile = withContext(Dispatchers.IO) {
                fileDownloader.download(version.downloadUrl, apkFile)
            }
            
            progressJob?.cancel()
            stateJob?.cancel()
            
            // 如果下载完成，状态已由 stateJob 更新
            if (downloadedFile == null && _updateState.value !is UpdateState.Downloaded) {
                val error = when (val state = downloadState.value) {
                    is DownloadState.Failed -> state.error
                    else -> "下载失败"
                }
                _updateState.value = UpdateState.DownloadFailed(error)
            }
        }
    }
    
    /**
     * 安装 APK
     */
    fun installApk(apkFile: File) {
        val success = updateInstaller.install(apkFile)
        if (success) {
            _updateState.value = UpdateState.Installing
        } else {
            _updateState.value = UpdateState.InstallFailed("无法启动安装程序")
        }
    }
    
    /**
     * 取消下载
     */
    fun cancelDownload() {
        fileDownloader.reset()
        _updateState.value = UpdateState.Idle
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        fileDownloader.reset()
        _latestVersion = null
        _updateState.value = UpdateState.Idle
    }
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class HasUpdate(val version: NewVersion) : UpdateState()
    data class CheckFailed(val error: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val file: File) : UpdateState()
    data class DownloadFailed(val error: String) : UpdateState()
    object Installing : UpdateState()
    data class InstallFailed(val error: String) : UpdateState()
}
