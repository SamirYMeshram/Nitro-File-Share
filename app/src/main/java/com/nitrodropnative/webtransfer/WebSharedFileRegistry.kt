package com.nitrodropnative.webtransfer

import com.nitrodropnative.storage.SelectedFile
import java.util.concurrent.atomic.AtomicLong

/**
 * Single source of truth for files shared through Web Transfer.
 *
 * The server never receives a stale copy of the share list. Every HTML/API/download
 * request calls [snapshot] or [get] so files added after server start become visible
 * immediately without restarting the server.
 */
internal class WebSharedFileRegistry {
    private val nextId = AtomicLong(1L)
    private val lock = Any()
    private var files: List<WebSharedFile> = emptyList()

    fun replace(newFiles: List<SelectedFile>): List<WebSharedFile> = synchronized(lock) {
        val previousByStableKey = files.associateBy { it.stableKey() }
        files = newFiles.map { selected ->
            val stableKey = selected.stableKey()
            val previous = previousByStableKey[stableKey]
            WebSharedFile(
                id = previous?.id ?: "f${nextId.getAndIncrement()}",
                uri = selected.uri,
                name = selected.name,
                size = selected.size,
                mimeType = selected.mimeType.ifBlank { "application/octet-stream" }
            )
        }
        files
    }

    fun add(newFiles: List<SelectedFile>): List<WebSharedFile> = synchronized(lock) {
        if (newFiles.isEmpty()) return@synchronized files
        val existingKeys = files.mapTo(mutableSetOf()) { it.stableKey() }
        val appended = newFiles
            .filter { existingKeys.add(it.stableKey()) }
            .map { selected ->
                WebSharedFile(
                    id = "f${nextId.getAndIncrement()}",
                    uri = selected.uri,
                    name = selected.name,
                    size = selected.size,
                    mimeType = selected.mimeType.ifBlank { "application/octet-stream" }
                )
            }
        files = files + appended
        files
    }

    fun remove(id: String): List<WebSharedFile> = synchronized(lock) {
        files = files.filterNot { it.id == id }
        files
    }

    fun clear(): List<WebSharedFile> = synchronized(lock) {
        files = emptyList()
        files
    }

    fun snapshot(): List<WebSharedFile> = synchronized(lock) { files.toList() }

    fun get(id: String): WebSharedFile? = synchronized(lock) { files.firstOrNull { it.id == id } }

    private fun WebSharedFile.stableKey(): String = "${uri}|$name|$size|$mimeType"

    private fun SelectedFile.stableKey(): String = "${uri}|$name|$size|$mimeType"
}
