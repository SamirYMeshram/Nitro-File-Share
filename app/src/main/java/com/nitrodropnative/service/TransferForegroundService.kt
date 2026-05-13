package com.nitrodropnative.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.nitrodropnative.core.constants.AppConstants

class TransferForegroundService : Service() {
    private lateinit var notificationManager: TransferNotificationManager
    private var wifiLock: WifiPerformanceLock? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = TransferNotificationManager(this)
        notificationManager.ensureChannel()
        wifiLock = WifiPerformanceLock(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "NitroDrop transfer"
                wifiLock?.acquire()
                val type = if (Build.VERSION.SDK_INT >= 34) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else 0
                ServiceCompat.startForeground(
                    this,
                    AppConstants.NOTIFICATION_ID,
                    notificationManager.buildInitial(fileName),
                    type
                )
            }
            ACTION_STOP, ACTION_CANCEL -> stopSelfSafely()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        wifiLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSelfSafely() {
        wifiLock?.release()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.nitrodropnative.transfer.START"
        const val ACTION_STOP = "com.nitrodropnative.transfer.STOP"
        const val ACTION_CANCEL = "com.nitrodropnative.transfer.CANCEL"
        const val EXTRA_FILE_NAME = "file_name"

        fun start(context: Context, fileName: String) {
            val intent = Intent(context, TransferForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TransferForegroundService::class.java).apply { action = ACTION_STOP })
        }
    }
}
