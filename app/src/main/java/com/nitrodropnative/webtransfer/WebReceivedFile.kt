package com.nitrodropnative.webtransfer

/**
 * File received from the PC browser during the current Web Transfer session.
 * The uri is a MediaStore content Uri created by the app, so it can be opened/shared
 * with ACTION_VIEW / ACTION_SEND using FLAG_GRANT_READ_URI_PERMISSION.
 */
data class WebReceivedFile(
    val id: String,
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val uri: String,
    val savedLocation: String,
    val receivedFrom: String,
    val averageSpeed: Long,
    val peakSpeed: Long,
    val durationSeconds: Long,
    val timestampMillis: Long,
    val checksumStatus: String = "Not checked"
) {
    val primaryActionLabel: String
        get() = when {
            mimeType.startsWith("video/") -> "Play video"
            mimeType.startsWith("audio/") -> "Play audio"
            mimeType.startsWith("image/") -> "View image"
            mimeType == "application/pdf" -> "Open PDF"
            mimeType == "application/vnd.android.package-archive" -> "Install APK"
            else -> "Open file"
        }

    val fileTypeLabel: String
        get() = when {
            mimeType.startsWith("video/") -> "Video"
            mimeType.startsWith("audio/") -> "Audio"
            mimeType.startsWith("image/") -> "Image"
            mimeType == "application/pdf" -> "PDF"
            mimeType == "application/vnd.android.package-archive" -> "APK"
            mimeType.contains("zip") || fileName.endsWith(".zip", ignoreCase = true) -> "Archive"
            else -> "File"
        }
}
