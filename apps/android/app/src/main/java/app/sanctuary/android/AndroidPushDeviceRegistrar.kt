package app.sanctuary.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.sanctuary.android.data.SessionRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidPushDeviceRegistrar(
    private val context: Context,
    private val repository: SessionRepository
) {
    suspend fun registerCurrentDevice() {
        FirebaseApp.initializeApp(context)
        val token = FirebaseMessaging.getInstance().awaitTokenWithRetry()
        registerToken(token)
    }

    suspend fun anonymousPushTokenIfAvailable(): String? {
        FirebaseApp.initializeApp(context)
        return runCatching { FirebaseMessaging.getInstance().awaitTokenWithRetry() }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    suspend fun registerToken(token: String) {
        repository.registerDevice(
            fcmToken = token,
            notificationsEnabled = notificationsEnabled()
        )
    }

    fun notificationsEnabled(): Boolean {
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

private suspend fun FirebaseMessaging.awaitTokenWithRetry(): String {
    var lastFailure: Throwable? = null
    repeat(4) { attempt ->
        if (attempt > 0) {
            delay(500)
        }

        try {
            val token = awaitToken().trim()
            if (token.isNotEmpty()) {
                return token
            }
        } catch (error: Throwable) {
            lastFailure = error
        }
    }

    lastFailure?.let { throw it }
    return awaitToken().trim()
}
