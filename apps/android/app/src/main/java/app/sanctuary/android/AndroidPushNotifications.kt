package app.sanctuary.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sanctuary.android.data.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

object AndroidPushNotifications {
    private fun preferences(context: Context) = context.getSharedPreferences("sanctuary_push", Context.MODE_PRIVATE)
    fun enabled(context: Context) = preferences(context).getBoolean("announcements", false)
    fun setEnabled(context: Context, enabled: Boolean) { preferences(context).edit().putBoolean("announcements", enabled).apply() }
    @Synchronized fun installation(context: Context): String {
        val prefs = preferences(context)
        return prefs.getString("installation", null) ?: UUID.randomUUID().toString().also { check(prefs.edit().putString("installation", it).commit()) }
    }
    fun allowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    fun channel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("sanctuary-announcements", "Sanctuary announcements", NotificationManager.IMPORTANCE_DEFAULT))
    }
    @Synchronized private fun secret(context: Context): String {
        val store = androidx.security.crypto.EncryptedSharedPreferences.create(context,"sanctuary_push_credentials",
            androidx.security.crypto.MasterKey.Builder(context).setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build(),
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        return store.getString("secret",null) ?: (UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","")).also { store.edit().putString("secret",it).commit() }
    }
    @Synchronized private fun nextRevision(context: Context): Long {
        val store=preferences(context);val revision=store.getLong("revision",0)+1
        store.edit().putLong("revision",revision).commit();return revision
    }
    suspend fun sync(context: Context, repository: SessionRepository) {
        // Play pre-launch crawlers must not become real announcement subscribers.
        if (android.provider.Settings.System.getString(context.contentResolver, "firebase.test.lab") == "true") return
        val revision=nextRevision(context)
        if (!enabled(context) && !preferences(context).getBoolean("registered",false)) return
        channel(context)
        val token = FirebaseMessaging.getInstance().token.await()
        repository.registerPushDevice(PushDeviceRequest(installation(context),token,"android",BuildConfig.VERSION_NAME,enabled(context) && allowed(context),secret(context),revision))
        preferences(context).edit().putBoolean("registered",true).apply()
    }
    fun forget(context: Context) {
        setEnabled(context,false)
        preferences(context).edit().putBoolean("registered",false).apply()
        FirebaseMessaging.getInstance().deleteToken()
    }
    fun show(context: Context, message: RemoteMessage) {
        if (!enabled(context) || !allowed(context)) return
        val destination = message.data["destination"] ?: "https://mydailysanctuary.com/"
        val uri = Uri.parse(destination)
        if (!isSanctuaryNotificationLink(destination, BuildConfig.ENVIRONMENT)) return
        channel(context)
        val id = (message.data["messageId"] ?: message.messageId ?: UUID.randomUUID().toString()).hashCode()
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context,"sanctuary-announcements")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(message.notification?.title ?: "Sanctuary")
            .setContentText(message.notification?.body).setStyle(NotificationCompat.BigTextStyle().bigText(message.notification?.body))
            .setAutoCancel(true).setContentIntent(pending).build()
        NotificationManagerCompat.from(context).notify(id,notification)
    }
}

class SanctuaryFirebaseMessagingService: FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob()+Dispatchers.IO)
    override fun onNewToken(token: String) {
        val repository = SessionRepository(applicationContext,SanctuaryApiFactory.create { null })
        scope.launch { runCatching { AndroidPushNotifications.sync(applicationContext,repository) } }
    }
    override fun onMessageReceived(message: RemoteMessage) { AndroidPushNotifications.show(applicationContext,message) }
    override fun onDestroy() { scope.cancel();super.onDestroy() }
}


internal fun isSanctuaryNotificationLink(value: String, environment: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme == "https" && (uri.host == "mydailysanctuary.com" || uri.host == "www.mydailysanctuary.com" ||
        (environment == "dev" && uri.host == "dev.mydailysanctuary.com"))
}.getOrDefault(false)
