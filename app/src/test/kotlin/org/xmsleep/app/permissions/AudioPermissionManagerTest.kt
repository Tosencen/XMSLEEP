package org.xmsleep.app.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AudioPermissionManagerTest {
    
    private lateinit var context: Context
    private lateinit var permissionManager: AudioPermissionManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionManager = AudioPermissionManager(context)
    }
    
    @Test
    fun `checkStoragePermission returns GRANTED when permission is granted`() {
        // Given
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_GRANTED
        
        // When
        val result = permissionManager.checkStoragePermission()
        
        // Then
        assertEquals(PermissionStatus.GRANTED, result)
    }
    
    @Test
    fun `checkStoragePermission returns NOT_REQUESTED when permission not requested before`() {
        // Given
        mockkStatic(ContextCompat::class)
        every { 
            ContextCompat.checkSelfPermission(context, any()) 
        } returns PackageManager.PERMISSION_DENIED
        
        val sharedPrefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.getBoolean("has_requested_audio_permission", false) } returns false
        
        // When
        val result = permissionManager.checkStoragePermission()
        
        // Then
        assertEquals(PermissionStatus.NOT_REQUESTED, result)
    }
    
    @Test
    fun `getSupportedFormats returns expected audio formats`() {
        // Given
        val importManager = org.xmsleep.app.audio.AudioImportManager(context)
        
        // When
        val formats = importManager.getSupportedFormats()
        
        // Then
        assertTrue(formats.contains("mp3"))
        assertTrue(formats.contains("ogg"))
        assertTrue(formats.contains("wav"))
        assertTrue(formats.contains("m4a"))
        assertTrue(formats.contains("aac"))
        assertTrue(formats.contains("flac"))
    }
}