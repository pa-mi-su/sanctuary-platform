package app.sanctuary.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sanctuary.android.data.SessionRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPushDeviceRegistrar(
    private val context: Context,
    private val repository: SessionRepository
) {
    suspend fun registerCurrentDevice() {
        val token = FirebaseMessaging.getInstance().awaitToken()
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        repository.registerDevice(
            fcmToken = token,
            notificationsEnabled = notificationsEnabled()
        )
    }

    private fun notificationsEnabled(): Boolean {
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

private suspend fun FirebaseMessaging.awaitToken(): String =
    suspendCancellableCoroutine { continuation ->
        token
            .addOnSuccessListener { value -> continuation.resume(value) }
            .addOnFailureListener { exception -> continuation.resumeWithException(exception) }
    }
