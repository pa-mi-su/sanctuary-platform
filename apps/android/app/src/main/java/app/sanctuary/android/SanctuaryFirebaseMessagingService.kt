package app.sanctuary.android

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sanctuary.android.data.SanctuaryApiFactory
import app.sanctuary.android.data.SessionRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SanctuaryFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Sanctuary"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showAdminNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val repository = SessionRepository(
            context = applicationContext,
            api = SanctuaryApiFactory.create { null }
        )
        val registrar = AndroidPushDeviceRegistrar(
            context = applicationContext,
            repository = repository
        )
        scope.launch {
            runCatching {
                registrar.registerToken(token)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun showAdminNotification(title: String, body: String) {
        if (!areNotificationsAllowed()) return
        AndroidNotificationChannels.ensureAdminUpdatesChannel(this)

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            ADMIN_NOTIFICATION_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AndroidNotificationChannels.ADMIN_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(this).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    private fun areNotificationsAllowed(): Boolean {
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return permissionGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    companion object {
        private const val ADMIN_NOTIFICATION_REQUEST_CODE = 8801
    }
}
