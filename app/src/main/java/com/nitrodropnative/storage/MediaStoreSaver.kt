package com.nitrodropnative.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nitrodropnative.core.constants.AppConstants
import java.io.File

object MediaStoreSaver {
    fun saveToDownloads(context: Context, source: File, displayName: String, mimeType: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.SIZE, source.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, AppConstants.DOWNLOADS_FOLDER)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create MediaStore download entry")
        resolver.openOutputStream(uri, "w")?.use { out ->
            source.inputStream().use { input -> input.copyTo(out, bufferSize = 1024 * 1024) }
        } ?: error("Unable to open MediaStore output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
