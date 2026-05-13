package com.nitrodropnative.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferHistoryEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileSize: Long,
    val direction: String,
    val receiverName: String,
    val senderName: String,
    val transportType: String,
    val averageSpeed: Long,
    val peakSpeed: Long,
    val duration: Long,
    val status: String,
    val timestamp: Long,
    val savedPath: String?
)
