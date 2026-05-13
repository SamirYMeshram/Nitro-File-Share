package com.nitrodropnative.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nitrodropnative.storage.FileReader
import com.nitrodropnative.storage.SelectedFile
import com.nitrodropnative.webtransfer.WebReceivedFile
import com.nitrodropnative.webtransfer.WebTransferManager
import com.nitrodropnative.webtransfer.WebTransferStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WebTransferViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = WebTransferManager(application)
    val stats: StateFlow<WebTransferStats> = manager.stats
    val sharedFiles: StateFlow<List<SelectedFile>> = manager.sharedFiles

    /**
     * Replaces the current phone-to-PC share list. This can be called before or
     * after the server is running; the running server reads the live registry.
     */
    fun setShareUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = uris.mapNotNull { uri ->
                persistReadPermission(uri)
                runCatching { FileReader.info(getApplication(), uri) }.getOrNull()
            }
            manager.replaceSharedFiles(files)
        }
    }

    /**
     * Appends files without restarting the HTTP server.
     */
    fun addShareUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val files = uris.mapNotNull { uri ->
                persistReadPermission(uri)
                runCatching { FileReader.info(getApplication(), uri) }.getOrNull()
            }
            manager.addSharedFiles(files)
        }
    }

    fun clearSharedFiles() {
        manager.clearSharedFiles()
    }

    fun start() {
        manager.start()
    }

    fun stop() {
        manager.stop()
    }

    fun deleteReceivedFile(file: WebReceivedFile, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = runCatching {
                val rows = getApplication<Application>().contentResolver.delete(Uri.parse(file.uri), null, null)
                rows > 0
            }.getOrDefault(false)
            if (deleted) manager.removeReceivedFile(file.uri)
            launch(Dispatchers.Main) { onResult(deleted) }
        }
    }

    private fun persistReadPermission(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    override fun onCleared() {
        manager.stop()
        super.onCleared()
    }
}
