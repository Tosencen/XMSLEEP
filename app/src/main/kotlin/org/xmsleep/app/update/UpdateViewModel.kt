package org.xmsleep.app.update

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmsleep.app.utils.Logger
import java.io.File
import java.io.IOException

/**
 * 更新状态管理 ViewModel
 */
class UpdateViewModel(private val app: Application) : AndroidViewModel(app) {
    
    private val updateChecker = UpdateChecker()
    private val fileDownloader = FileDownloader()
    private val updateInstaller = UpdateInstaller(app)
    
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    val downloadProgress = fileDownloader.progress
    val downloadState = fileDownloader.state
    
    private var _latestVersion: NewVersion? = null
    val latestVersion: NewVersion?
        get() = _latestVersion
    
    private var lastCheckTime: Long = 0L
    
    // SharedPreferences 用于持久化下载状态
    private val prefs = app.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
    
    /**
     * 自动检查更新（带时间间隔限制，1小时内只检查一次）
     */
    fun startAutomaticCheckLatestVersion(currentVersion: String) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCheck = currentTime - lastCheckTime
        // 1小时内只检查一次
        if (lastCheckTime > 0 && timeSinceLastCheck < 1000 * 60 * 60 * 1) {
            Logger.d("UpdateCheck", "距离上次检查仅 ${timeSinceLastCheck / 1000 / 60} 分钟，跳过本次检查")
            return
        }
        Logger.d("UpdateCheck", "开始检查更新，当前版本: $currentVersion")
        checkUpdate(currentVersion)
    }
    
    /**
     * 检查更新
     * @param currentVersion 当前版本号
     */
    fun checkUpdate(currentVersion: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            lastCheckTime = System.currentTimeMillis()
            Logger.d("UpdateCheck", "开始调用UpdateChecker.checkLatestVersion，当前版本: $currentVersion")
            try {
                val newVersion = withContext(Dispatchers.IO) {
                    updateChecker.checkLatestVersion(currentVersion)
                }
                
                Logger.d("UpdateCheck", "UpdateChecker返回结果: ${if (newVersion != null) "有新版本 ${newVersion.version}" else "无新版本"}")
                
                if (newVersion != null) {
                    _latestVersion = newVersion
                    
                    // 检查是否已下载该版本
                    val downloadedFile = getDownloadedApkFile(newVersion.version)
                    if (downloadedFile != null && downloadedFile.exists() && isValidApk(downloadedFile)) {
                        // 已下载且文件有效，直接进入已下载状态
                        Logger.d("UpdateCheck", "检测到已下载的APK文件: ${downloadedFile.path}")
                        _updateState.value = UpdateState.Downloaded(downloadedFile)
                    } else {
                        // 未下载或文件无效，清理旧文件
                        if (downloadedFile != null && downloadedFile.exists()) {
                            downloadedFile.delete()
                            Logger.d("UpdateCheck", "删除无效的APK文件")
                        }
                        clearDownloadState()
                        _updateState.value = UpdateState.HasUpdate(newVersion)
                    }
                    
                    Logger.d("UpdateCheck", "已设置更新状态")
                } else {
                    _updateState.value = UpdateState.UpToDate
                    Logger.d("UpdateCheck", "已设置更新状态为UpToDate")
                }
            } catch (e: IOException) {
                // 网络错误或 rate limit 错误
                val errorMsg = e.message ?: "网络连接失败，请检查网络后重试"
                Logger.e("UpdateCheck", "检查更新失败 (IOException): $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            } catch (e: Exception) {
                val errorMsg = e.message ?: "检查更新失败"
                Logger.e("UpdateCheck", "检查更新失败 (Exception): $errorMsg", e)
                _updateState.value = UpdateState.CheckFailed(errorMsg)
            }
        }
    }
    
    /**
     * 获取已下载的APK文件
     */
    private fun getDownloadedApkFile(version: String): File? {
        val downloadedVersion = prefs.getString("downloaded_version", null)
        val downloadedPath = prefs.getString("downloaded_path", null)
        
        if (downloadedVersion == version && downloadedPath != null) {
            return File(downloadedPath)
        }
        return null
    }
    
    /**
     * 验证APK文件是否有效（存在且签名与当前应用一致，防止供应链投毒）
     */
    private fun isValidApk(file: File): Boolean {
        return try {
            if (!(file.exists() && file.length() > 0 && file.canRead())) {
                Logger.w("UpdateCheck", "APK文件不存在或为空: ${file.path}")
                return false
            }
            val result = verifyApkSignatureMatchesCurrentApp(file)
            if (!result) {
                Logger.e("UpdateCheck", "APK签名校验失败，拒绝安装: ${file.path}")
            }
            result
        } catch (e: Exception) {
            Logger.e("UpdateCheck", "APK验证异常: ${e.message}", e)
            false
        }
    }

    /**
     * 将下载的 APK 签名与当前已安装应用签名比对
     */
    private fun verifyApkSignatureMatchesCurrentApp(apkFile: File): Boolean {
        val currentSignatures = getInstalledAppSignatures() ?: return false
        val pm = app.packageManager

        val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
        }
        if (archiveInfo == null) {
            Logger.e("UpdateCheck", "无法解析下载的APK元信息")
            return false
        }

        val apkSignatures = extractSignatures(archiveInfo) ?: run {
            Logger.e("UpdateCheck", "下载的APK无签名信息")
            return false
        }

        val currentCerts = currentSignatures.map { certificateDigest(it) }.toSet()
        return apkSignatures.any { certificateDigest(it) in currentCerts }
    }

    /**
     * 获取当前已安装应用的签名证书
     */
    private fun getInstalledAppSignatures(): List<android.content.pm.Signature>? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES)
            }
            extractSignatures(info)
        } catch (e: Exception) {
            Logger.e("UpdateCheck", "获取当前应用签名失败: ${e.message}", e)
            null
        }
    }

    /**
     * 统一提取 PackageInfo 的签名证书（API 28+ 用 signingInfo，低版本用 signatures）
     */
    private fun extractSignatures(info: android.content.pm.PackageInfo): List<android.content.pm.Signature>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return null
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.toList()
            } else {
                signingInfo.signingCertificateHistory.toList()
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList()
        }
    }

    /**
     * 计算签名证书的 SHA-256 摘要（十六进制）
     */
    private fun certificateDigest(signature: android.content.pm.Signature): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 保存下载状态
     */
    private fun saveDownloadState(version: String, filePath: String) {
        prefs.edit()
            .putString("downloaded_version", version)
            .putString("downloaded_path", filePath)
            .apply()
        Logger.d("UpdateCheck", "保存下载状态: version=$version, path=$filePath")
    }
    
    /**
     * 清除下载状态
     */
    private fun clearDownloadState() {
        prefs.edit()
            .remove("downloaded_version")
            .remove("downloaded_path")
            .apply()
        Logger.d("UpdateCheck", "清除下载状态")
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload() {
        val version = _latestVersion ?: return
        
        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(0f)
            
            // 获取下载目录
            val downloadDir = File(app.getExternalFilesDir(null), "updates")
            downloadDir.mkdirs()
            
            val apkFile = File(downloadDir, "XMSLEEP-${version.version}.apk")
            
            // 监听下载进度和状态
            var progressJob: Job? = null
            var stateJob: Job? = null
            
            progressJob = viewModelScope.launch {
                downloadProgress.collect { progress ->
                    _updateState.value = UpdateState.Downloading(progress)
                }
            }
            
            stateJob = viewModelScope.launch {
                downloadState.collect { state ->
                    when (state) {
                        is DownloadState.Success -> {
                            // 保存下载状态
                            saveDownloadState(version.version, state.file.absolutePath)
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
            
            // 下载文件（带智能回退）
            val downloadedFile = withContext(Dispatchers.IO) {
                fileDownloader.download(version.downloadUrls, apkFile)
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
    
    // 保存待安装的 APK 文件
    private var pendingInstallFile: File? = null
    
    /**
     * 检查是否有待安装的文件且权限已授予
     * 用于应用恢复时自动检测
     */
    fun checkPendingInstall(): Boolean {
        val file = pendingInstallFile
        if (file != null && file.exists()) {
            // 如果有待安装的文件，检查权限
            if (updateInstaller.hasInstallPermission()) {
                // 权限已授予，可以安装
                return true
            }
        }
        return false
    }
    
    /**
     * 自动重试安装（用于应用恢复时）
     */
    fun autoRetryInstall() {
        val file = pendingInstallFile
        if (file != null && file.exists() && updateInstaller.hasInstallPermission()) {
            // 有文件且权限已授予，自动安装
            installApk(file)
        }
    }
    
    /**
     * 安装 APK
     */
    fun installApk(apkFile: File) {
        // 保存待安装的文件
        pendingInstallFile = apkFile
        
        // 检查安装权限
        if (!updateInstaller.hasInstallPermission()) {
            // 没有权限，请求权限
            updateInstaller.requestInstallPermission()
            _updateState.value = UpdateState.InstallPermissionRequested
            return
        }
        
        val success = updateInstaller.install(apkFile)
        if (success) {
            _updateState.value = UpdateState.Installing
            // 安装成功后清除下载状态（在下次启动时会检测到已安装最新版本）
            clearDownloadState()
            pendingInstallFile = null
        } else {
            _updateState.value = UpdateState.InstallFailed("无法启动安装程序，请检查文件是否完整")
            pendingInstallFile = null
        }
    }
    
    /**
     * 重试安装（在用户授予权限后调用）
     */
    fun retryInstall() {
        val file = pendingInstallFile
        if (file != null && file.exists()) {
            installApk(file)
        } else {
            // 如果文件不存在，尝试从当前状态获取
            val currentState = _updateState.value
            if (currentState is UpdateState.Downloaded) {
                installApk(currentState.file)
            } else {
                _updateState.value = UpdateState.InstallFailed("找不到 APK 文件")
            }
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
    object InstallPermissionRequested : UpdateState()
    object Installing : UpdateState()
    data class InstallFailed(val error: String) : UpdateState()
}
