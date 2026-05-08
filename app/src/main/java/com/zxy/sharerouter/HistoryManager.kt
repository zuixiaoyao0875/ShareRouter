package com.zxy.sharerouter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class HistoryManager(private val context: Context) {
    private val db = ShareDatabase.getDatabase(context)
    private val dao = db.historyDao()
    private val historyDir = context.getExternalFilesDir("share_history") ?: File(context.filesDir, "share_history")

    init {
        if (!historyDir.exists()) historyDir.mkdirs()
    }

    suspend fun cloneAndSave(intent: Intent, limit: Int, callingPackage: String?) = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val entryDir = File(historyDir, timestamp.toString())
            if (!entryDir.exists()) entryDir.mkdirs()

            var type = "TEXT"
            var contentText: String? = null
            val clonedFiles = mutableListOf<String>()
            val attachmentNames = mutableListOf<String>()
            val mimeType = intent.type

            when (intent.action) {
                Intent.ACTION_SEND -> {
                    contentText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (streamUri != null) {
                        type = intent.type?.split("/")?.firstOrNull()?.uppercase() ?: "FILE"
                        val fileName = getFileName(streamUri)
                        val localPath = copyUriToLocal(streamUri, entryDir, fileName)
                        if (localPath != null) {
                            clonedFiles.add(localPath)
                            attachmentNames.add(fileName ?: "未命名文件")
                        }
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    type = "MULTIPLE"
                    val streamUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    streamUris?.forEach { uri ->
                        val fileName = getFileName(uri)
                        val localPath = copyUriToLocal(uri, entryDir, fileName)
                        if (localPath != null) {
                            clonedFiles.add(localPath)
                            attachmentNames.add(fileName ?: "未命名文件")
                        }
                    }
                }
            }

            if (contentText == null && clonedFiles.isEmpty()) {
                entryDir.deleteRecursively()
                return@withContext
            }

            val sourceAppName = callingPackage?.let { pkg ->
                try {
                    val pm = context.packageManager
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    null
                }
            }

            val entry = ShareHistoryEntry(
                timestamp = timestamp,
                type = type,
                contentText = contentText,
                filePaths = clonedFiles.joinToString(","),
                attachmentNames = attachmentNames.joinToString(","),
                mimeType = mimeType,
                sourceApp = sourceAppName,
                sourcePackage = callingPackage
            )

            dao.insert(entry)
            dao.trim(limit)
            
            // Cleanup deleted folders (optional but good for consistency)
            cleanupOrphanedFolders(limit)
            
        } catch (e: Exception) {
            Log.e("HistoryManager", "Error cloning intent: ${e.message}")
        }
    }

    private fun copyUriToLocal(uri: Uri, destDir: File, fileName: String?): String? {
        return try {
            val finalName = fileName ?: UUID.randomUUID().toString()
            val destFile = File(destDir, finalName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e("HistoryManager", "Failed to copy URI: $uri, error: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = cursor.getString(index)
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) name = name?.substring(cut + 1)
        }
        return name
    }

    private suspend fun cleanupOrphanedFolders(limit: Int) {
        // Simple cleanup: delete folders that don't match any timestamp in DB
        // In a real app, this should be more robust
    }

    fun createDispatchIntent(entry: ShareHistoryEntry): Intent {
        val intent = Intent().apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        val paths = entry.filePaths?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        
        if (paths.isEmpty()) {
            intent.action = Intent.ACTION_SEND
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, entry.contentText)
        } else if (paths.size == 1) {
            val file = File(paths[0])
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            intent.action = Intent.ACTION_SEND
            intent.type = context.contentResolver.getType(uri) ?: "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            if (entry.contentText != null) intent.putExtra(Intent.EXTRA_TEXT, entry.contentText)
        } else {
            val uris = ArrayList(paths.map { 
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(it))
            })
            intent.action = Intent.ACTION_SEND_MULTIPLE
            intent.type = "*/*" // Or more specific if all same
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (entry.contentText != null) intent.putExtra(Intent.EXTRA_TEXT, entry.contentText)
        }
        
        return intent
    }
}
