package app.sanctuary.android

import app.sanctuary.android.data.SanctuaryApiFactory
import app.sanctuary.android.data.SessionRepository
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SanctuaryFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
}
