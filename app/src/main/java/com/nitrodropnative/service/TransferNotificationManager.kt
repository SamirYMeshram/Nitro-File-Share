package com.nitrodropnative.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nitrodropnative.MainActivity
import com.nitrodropnative.R
import com.nitrodropnative.core.constants.AppConstants
import com.nitrodropnative.core.util.SpeedFormatter
import com.nitrodropnative.transfer.TransferStats

class TransferNotificationManager(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                "NitroDrop transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active device-to-device file transfers"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun buildInitial(fileName: String): Notification = base(fileName)
        .setContentText("Preparing high-speed transfer")
        .setProgress(100, 0, true)
        .build()

    fun buildProgress(stats: TransferStats): Notification {
        val percent = (stats.progressPercent * 100).toInt().coerceIn(0, 100)
        return base(stats.fileName.ifBlank { "NitroDrop transfer" })
            .setContentText("$percent% • ${SpeedFormatter.format(stats.currentSpeed)}")
            .setProgress(100, percent, false)
            .build()
    }

    fun notify(stats: TransferStats) {
        manager.notify(AppConstants.NOTIFICATION_ID, buildProgress(stats))
    }

    private fun base(title: String): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 100, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelIntent = Intent(context, TransferForegroundService::class.java).apply {
            action = TransferForegroundService.ACTION_CANCEL
        }
        val cancelPending = PendingIntent.getService(
            context, 101, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentIntent(pending)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, "Cancel", cancelPending)
    }
}
