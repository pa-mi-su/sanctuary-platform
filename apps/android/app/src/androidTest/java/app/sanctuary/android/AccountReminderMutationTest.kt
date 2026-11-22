package app.sanctuary.android

import androidx.test.platform.app.InstrumentationRegistry
import android.os.ParcelFileDescriptor
import app.sanctuary.android.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AccountReminderMutationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository by lazy { SessionRepository(context, SanctuaryApiFactory.create { null }) }
    private val scheduler by lazy { AndroidReminderScheduler(context) }
    private val prayer = "https://mydailysanctuary.com/prayers/qa-prayer"
    private val novena = "https://mydailysanctuary.com/novenas/qa-novena"

    private fun nativeAlarmCount(): Int {
        val dump = ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys alarm")
        ).bufferedReader().use { it.readText() }
        return dump.split(Regex("\\n(?=    (?:RTC|ELAPSED))"))
            .count { it.lineSequence().firstOrNull()?.contains(context.packageName) == true && it.contains("ACTION_SHOW_REMINDER") }
    }

    @Before fun seed() = runBlocking {
        ReminderFixture.auth(200)
        ReminderFixture.reminderMutations(200)
        assertTrue(scheduler.preciseTimingAllowed())
        assertTrue(scheduler.notificationsAllowed())
        val session = repository.login("qa@example.test", "local-fixture-only")
        withContext(Dispatchers.Main) {
            scheduler.cancelAll()
            AccountReminderSync.configure(context, repository, session.profile!!.userId)
        }
    }

    @After fun clean() = runBlocking {
        ReminderFixture.reminderMutations(200)
        withContext(Dispatchers.Main) { AccountReminderSync.configure(context, repository, null) }
        repository.logout()
    }

    @Test fun confirmedWritesUpdateNativeAlarmsWhenListDownloadsFail() = runBlocking {
        withContext(Dispatchers.Main) {
            // Every subsequent GET /me/reminders returns 503.
            AccountReminderSync.save(prayer, "Prayer fixture", 15, 0)
            assertEquals(15 to 0, scheduler.itemTime(prayer))
            AccountReminderSync.save(novena, "Novena fixture", 15, 5)
            assertEquals(2, scheduler.itemReminders().size)
            assertEquals(2, nativeAlarmCount())
            AccountReminderSync.save(prayer, "Edited prayer", 16, 30)
            assertEquals(16 to 30, scheduler.itemTime(prayer))
            AccountReminderSync.remove(prayer)
            assertNull(scheduler.itemTime(prayer))
            assertEquals(15 to 5, scheduler.itemTime(novena))
            assertEquals(1, nativeAlarmCount())
            AccountReminderSync.clear()
            assertTrue(scheduler.itemReminders().isEmpty())
            assertEquals(0, nativeAlarmCount())
        }
    }

    @Test fun failedWritesCannotCreateOrRemoveLocalReminders() = runBlocking {
        withContext(Dispatchers.Main) { AccountReminderSync.save(prayer, "Retained prayer", 15, 0) }
        ReminderFixture.reminderMutations(503)
        withContext(Dispatchers.Main) {
            assertTrue(runCatching { AccountReminderSync.save(novena, "Rejected novena", 15, 5) }.isFailure)
            assertNull(scheduler.itemTime(novena))
            assertTrue(runCatching { AccountReminderSync.remove(prayer) }.isFailure)
            assertEquals(15 to 0, scheduler.itemTime(prayer))
            assertEquals(1, nativeAlarmCount())
            assertTrue(runCatching { AccountReminderSync.clear() }.isFailure)
            assertEquals(15 to 0, scheduler.itemTime(prayer))
        }
    }
}
