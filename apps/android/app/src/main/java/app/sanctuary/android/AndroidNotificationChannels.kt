package app.sanctuary.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AndroidNotificationChannels {
    const val ADMIN_UPDATES_CHANNEL_ID = "sanctuary-updates"

    fun ensureAdminUpdatesChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ADMIN_UPDATES_CHANNEL_ID,
            "Sanctuary updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Messages sent from Sanctuary administrators."
        }
        manager.createNotificationChannel(channel)
    }
}
