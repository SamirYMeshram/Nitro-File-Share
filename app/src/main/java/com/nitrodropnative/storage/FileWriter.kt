package com.nitrodropnative.storage

import java.io.File
import java.io.RandomAccessFile

object FileWriter {
    fun randomAccess(file: File, offset: Long): RandomAccessFile {
        file.parentFile?.mkdirs()
        return RandomAccessFile(file, "rw").apply { seek(offset) }
    }
}
