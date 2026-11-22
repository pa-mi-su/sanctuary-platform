package app.sanctuary.android

import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ReminderSessionRecoveryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository by lazy { SessionRepository(context, SanctuaryApiFactory.create { null }) }
    private val scheduler by lazy { AndroidReminderScheduler(context) }
    private val destination = "https://mydailysanctuary.com/prayers/qa-prayer"

    @Before fun seed() = runBlocking {
        ReminderFixture.auth(200)
        scheduler.cancelAll()
        val session = repository.login("qa@example.test", "local-fixture-only")
        assertTrue(session.authenticated)
        scheduler.setAccount(session.profile!!.userId)
        scheduler.reconcile(listOf(AccountContentReminder("prayers", "qa-prayer", "Offline reminder", 15, 0)))
    }

    @After fun clean() {
        ReminderFixture.auth(200)
        repository.logout()
        scheduler.cancelAll()
    }

    @Test fun profileOutagePreservesSessionAndLocalReminders() = runBlocking {
        ReminderFixture.auth(503)
        assertFalse(repository.bootstrap().authenticated)
        assertNotNull(repository.currentSession())
        assertEquals(15 to 0, scheduler.itemTime(destination))
    }

    @Test fun expiredAccessWithRefreshOutagePreservesSessionAndReminders() = runBlocking {
        ReminderFixture.auth(status = 401, refreshStatus = 503)
        assertFalse(repository.bootstrap().authenticated)
        assertNotNull(repository.currentSession())
        assertEquals(15 to 0, scheduler.itemTime(destination))
        ReminderFixture.auth(200)
        assertTrue(repository.bootstrap().authenticated)
    }

    @Test fun bootstrapTimeoutDoesNotEraseCredentialsOrReminders() = runBlocking {
        ReminderFixture.auth(200, delayMs = 500)
        assertNull(withTimeoutOrNull(100) { repository.bootstrap() })
        assertNotNull(repository.currentSession())
        assertEquals(15 to 0, scheduler.itemTime(destination))
    }

    @Test fun rejectedCredentialsAreStillCleared() = runBlocking {
        ReminderFixture.auth(401)
        assertFalse(repository.bootstrap().authenticated)
        assertNull(repository.currentSession())
    }
}
