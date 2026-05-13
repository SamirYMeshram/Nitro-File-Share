package com.nitrodropnative.transfer

import com.nitrodropnative.core.constants.AppConstants
import org.json.JSONObject

data class TransferMetadata(
    val protocolVersion: Int = AppConstants.PROTOCOL_VERSION,
    val sessionId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val lastModified: Long,
    val checksumType: String = "SHA-256",
    val chunkSize: Int = AppConstants.DEFAULT_CHUNK_SIZE,
    val totalFiles: Int = 1,
    val fileIndex: Int = 0
) {
    fun toJson(): String = JSONObject()
        .put("protocolVersion", protocolVersion)
        .put("sessionId", sessionId)
        .put("fileName", fileName)
        .put("fileSize", fileSize)
        .put("mimeType", mimeType)
        .put("lastModified", lastModified)
        .put("checksumType", checksumType)
        .put("chunkSize", chunkSize)
        .put("totalFiles", totalFiles)
        .put("fileIndex", fileIndex)
        .toString()

    companion object {
        fun fromJson(json: String): TransferMetadata {
            val obj = JSONObject(json)
            return TransferMetadata(
                protocolVersion = obj.getInt("protocolVersion"),
                sessionId = obj.getString("sessionId"),
                fileName = obj.getString("fileName"),
                fileSize = obj.getLong("fileSize"),
                mimeType = obj.optString("mimeType", "application/octet-stream"),
                lastModified = obj.optLong("lastModified", 0L),
                checksumType = obj.optString("checksumType", "SHA-256"),
                chunkSize = obj.optInt("chunkSize", AppConstants.DEFAULT_CHUNK_SIZE),
                totalFiles = obj.optInt("totalFiles", 1),
                fileIndex = obj.optInt("fileIndex", 0)
            )
        }
    }
}
