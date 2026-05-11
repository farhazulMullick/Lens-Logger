package io.github.farhazulmullick.lenslogger.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier

internal object LensNotificationHelper {

    /**
     * Bumped when channel importance changes so existing installs pick up the new behavior
     * without being stuck on an old low-importance (silent) channel.
     */
    internal const val CHANNEL_ID = "io.github.farhazulmullick.lenslogger.debug.v2"

    private const val NOTIFICATION_ID = 0x4C454E53 // "LENS"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.lens_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.lens_notification_channel_description)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    fun showPersistent(application: Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                application,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Napier.w {
                    "Lens: POST_NOTIFICATIONS not granted; persistent notification was not shown. " +
                        "Request the permission, then call LensAndroid.refreshPersistentNotification()."
                }
                return
            }
        }
        ensureChannel(application)
        val intent = LensActivity.createIntent(application)
        val pendingIntent = PendingIntent.getActivity(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(application, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(application.getString(R.string.lens_notification_title))
            .setContentText(application.getString(R.string.lens_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(false)
            .build()
        NotificationManagerCompat.from(application).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
