package com.nitrodropnative.webtransfer

import android.net.Uri
import com.nitrodropnative.storage.SelectedFile

/**
 * Server-facing shared file model.
 *
 * Browser routes must never expose raw file paths/URIs. The HTTP server uses [id]
 * as the only public identifier and resolves it through [WebSharedFileRegistry]
 * for every request.
 */
internal data class WebSharedFile(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String
) {
    fun toSelectedFile(): SelectedFile = SelectedFile(uri, name, size, mimeType)
}
