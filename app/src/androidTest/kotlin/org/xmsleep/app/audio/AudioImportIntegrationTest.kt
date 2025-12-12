package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.xmsleep.app.permissions.AudioPermissionManager
import org.xmsleep.app.permissions.PermissionStatus

@RunWith(AndroidJUnit4::class)
class AudioImportIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: AudioPermissionManager
    private lateinit var importManager: AudioImportManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        permissionManager = AudioPermissionManager(context)
        importManager = AudioImportManager(context)
    }
    
    @Test
    fun testPermissionManagerInitialization() {
        // 验证权限管理器可以正确初始化
        val status = permissionManager.checkStoragePermission()
        
        // 状态应该是有效的枚举值之一
        assert(
            status == PermissionStatus.GRANTED ||
            status == PermissionStatus.DENIED ||
            status == PermissionStatus.PERMANENTLY_DENIED ||
            status == PermissionStatus.NOT_REQUESTED
        )
    }
    
    @Test
    fun testImportManagerSupportedFormats() {
        // 验证导入管理器支持的格式
        val formats = importManager.getSupportedFormats()
        
        assert(formats.isNotEmpty())
        assert(formats.contains("mp3"))
        assert(formats.contains("ogg"))
    }
    
    @Test
    fun testValidationWithInvalidUri() = runBlocking {
        // 测试无效URI的验证
        val invalidUri = Uri.parse("content://invalid/uri")
        val result = importManager.validateAudioFile(invalidUri)
        
        assert(!result.isValid)
        assert(result.errorMessage != null)
    }
}