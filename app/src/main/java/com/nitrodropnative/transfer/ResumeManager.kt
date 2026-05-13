package com.nitrodropnative.transfer

import android.content.Context
import com.nitrodropnative.core.constants.AppConstants
import java.io.File

class ResumeManager(private val context: Context) {
    fun partialFile(metadata: TransferMetadata): File {
        val dir = File(context.getExternalFilesDir(null), "incoming")
        if (!dir.exists()) dir.mkdirs()
        val safeName = metadata.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "${metadata.sessionId}_$safeName${AppConstants.PARTIAL_EXTENSION}")
    }

    fun finalPrivateFile(metadata: TransferMetadata): File {
        val dir = File(context.getExternalFilesDir(null), "completed")
        if (!dir.exists()) dir.mkdirs()
        val safeName = metadata.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, safeName)
    }

    fun receivedBytes(metadata: TransferMetadata): Long {
        val file = partialFile(metadata)
        return if (file.exists()) file.length().coerceAtMost(metadata.fileSize) else 0L
    }
}
