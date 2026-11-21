package app.sanctuary.android

import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.AccountContentReminder
import app.sanctuary.android.data.SanctuaryApiFactory
import app.sanctuary.android.data.SessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Run against the 1.0.38 DEV APK, then install the candidate with -r (no clear/uninstall). */
class ReminderUpgradeFixtureTest {
    @Test fun seedPriorRelease() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".dev"))
        check(BuildConfig.API_BASE_URL.startsWith("http://10.0.2.2:18084")) { "Loopback fixture only" }
        val repository = SessionRepository(context, SanctuaryApiFactory.create { null })
        val session = repository.login("qa@example.test", "local-fixture-only")
        assertTrue(session.authenticated)
        // Instrumentation can terminate before SharedPreferences.apply reaches disk.
        // A synchronous write on the backing store flushes the queued login writes.
        assertTrue(context.getSharedPreferences("sanctuary_session", android.content.Context.MODE_PRIVATE)
            .edit().commit())
        assertNotNull(repository.currentSession())
        val scheduler = AndroidReminderScheduler(context)
        val permissionAllowed = InstrumentationRegistry.getArguments().getString("permissionAllowed") == "true"
        assertEquals("Baseline permission must match the upgrade scenario", permissionAllowed, scheduler.preciseTimingAllowed())
        scheduler.cancelAll()
        scheduler.setAccount(session.profile!!.userId)
        scheduler.reconcile(listOf(
            AccountContentReminder("prayers", "qa-prayer", "Devotion to the Seven Sorrows of Mary", 15, 0),
            AccountContentReminder("novenas", "qa-novena", "A novena with a deliberately long title for narrow screen layout", 15, 5)
        ))
        assertEquals(2, scheduler.itemReminders().size)
        assertTrue(AndroidPushNotifications.setEnabled(context, true))
    }
}
