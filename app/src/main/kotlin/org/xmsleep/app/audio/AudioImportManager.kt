package org.xmsleep.app.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import org.xmsleep.app.audio.model.SoundMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

data class ValidationResult(
    val isValid: Boolean,
    val format: String?,
    val duration: Long?,
    val errorMessage: String?
)

enum class ImportError {
    INVALID_FORMAT,
    FILE_NOT_FOUND,
    STORAGE_FULL,
    CORRUPTED_FILE,
    PERMISSION_DENIED,
    UNKNOWN_ERROR
}

interface ImportCallback {
    fun onImportSuccess(soundMetadata: SoundMetadata)
    fun onImportError(error: ImportError, message: String)
}

class AudioImportManager(private val context: Context) {
    
    companion object {
        private val SUPPORTED_FORMATS = listOf("mp3", "ogg", "wav", "m4a", "aac", "flac")
        private val SUPPORTED_MIME_TYPES = listOf(
            "audio/mpeg", "audio/ogg", "audio/wav", "audio/x-wav", 
            "audio/mp4", "audio/aac", "audio/flac"
        )
    }
    
    fun getSupportedFormats(): List<String> = SUPPORTED_FORMATS
    
    fun validateAudioFile(uri: Uri): ValidationResult {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            
            // 检查MIME类型
            if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
                val extension = getFileExtension(uri)
                if (extension == null || !SUPPORTED_FORMATS.contains(extension.lowercase())) {
                    return ValidationResult(
                        isValid = false,
                        format = null,
                        duration = null,
                        errorMessage = "不支持的音频格式。支持的格式：${SUPPORTED_FORMATS.joinToString(", ")}"
                    )
                }
            }
            
            // 使用MediaMetadataRetriever验证文件
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                val mimeTypeFromFile = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                
                if (duration == null || duration <= 0) {
                    return ValidationResult(
                        isValid = false,
                        format = null,
                        duration = null,
                        errorMessage = "无法读取音频文件信息，文件可能已损坏"
                    )
                }
                
                return ValidationResult(
                    isValid = true,
                    format = mimeTypeFromFile ?: mimeType,
                    duration = duration,
                    errorMessage = null
                )
                
            } finally {
                retriever.release()
            }
            
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                format = null,
                duration = null,
                errorMessage = "文件验证失败：${e.message}"
            )
        }
    }
    
    fun importAudioFile(uri: Uri, callback: ImportCallback) {
        try {
            // 首先验证文件
            val validation = validateAudioFile(uri)
            if (!validation.isValid) {
                callback.onImportError(ImportError.INVALID_FORMAT, validation.errorMessage ?: "文件格式无效")
                return
            }
            
            // 获取文件名
            val fileName = getFileName(uri) ?: "imported_audio_${System.currentTimeMillis()}"
            val extension = getFileExtension(uri) ?: "mp3"
            val finalFileName = "${UUID.randomUUID()}_$fileName"
            
            // 复制到内部存储
            val internalFile = copyToInternalStorage(uri, finalFileName)
            if (internalFile == null) {
                callback.onImportError(ImportError.STORAGE_FULL, "无法保存文件到内部存储")
                return
            }
            
            // 创建音频元数据
            val soundMetadata = SoundMetadata(
                id = UUID.randomUUID().toString(),
                name = fileName.substringBeforeLast('.'),
                category = "Imported", // 导入音频的分类
                source = org.xmsleep.app.audio.model.AudioSource.IMPORTED,
                importedPath = internalFile.absolutePath,
                importedDate = System.currentTimeMillis(),
                duration = validation.duration
            )
            
            callback.onImportSuccess(soundMetadata)
            
        } catch (e: SecurityException) {
            callback.onImportError(ImportError.PERMISSION_DENIED, "没有访问文件的权限")
        } catch (e: IOException) {
            callback.onImportError(ImportError.STORAGE_FULL, "存储空间不足或文件写入失败")
        } catch (e: Exception) {
            callback.onImportError(ImportError.UNKNOWN_ERROR, "导入失败：${e.message}")
        }
    }
    
    fun copyToInternalStorage(uri: Uri, fileName: String): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val internalDir = File(context.filesDir, "imported_audio")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            
            val outputFile = File(internalDir, fileName)
            val outputStream = FileOutputStream(outputFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            return outputFile
            
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
    
    private fun getFileExtension(uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: getFileName(uri)?.substringAfterLast('.', "")
    }
}