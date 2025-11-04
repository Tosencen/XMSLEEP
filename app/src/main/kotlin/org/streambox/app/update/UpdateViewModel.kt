package org.streambox.app.update

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

/**
 * 更新状态管理 ViewModel
 */
class UpdateViewModel(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val updateChecker = UpdateChecker()
    private val fileDownloader = FileDownloader()
    private val updateInstaller = UpdateInstaller(context)
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    val downloadProgress = fileDownloader.progress
    val downloadState = fileDownloader.state
    
    private var latestVersion: NewVersion? = null
    
    /**
     * 检查更新
     * @param currentVersion 当前版本号
     */
    fun checkUpdate(currentVersion: String) {
        scope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val newVersion = withContext(Dispatchers.IO) {
                    updateChecker.checkLatestVersion(currentVersion)
                }
                
                if (newVersion != null) {
                    latestVersion = newVersion
                    _updateState.value = UpdateState.HasUpdate(newVersion)
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.CheckFailed(e.message ?: "检查更新失败")
            }
        }
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload() {
        val version = latestVersion ?: return
        
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
        latestVersion = null
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
