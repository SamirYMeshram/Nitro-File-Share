package com.nitrodropnative.storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
)

object FileReader {
    fun info(context: Context, uri: Uri): SelectedFile {
        val resolver = context.contentResolver
        var name = "file_${System.currentTimeMillis()}"
        var size = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor: Cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
            }
        }
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        return SelectedFile(uri, name, size.coerceAtLeast(0L), mime)
    }

    fun open(context: Context, uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri) ?: error("Unable to open input stream")
}
