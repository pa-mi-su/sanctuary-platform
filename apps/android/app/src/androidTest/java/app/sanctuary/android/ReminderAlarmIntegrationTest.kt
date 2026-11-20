package app.sanctuary.android

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.AccountContentReminder
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Run on a disposable emulator with SCHEDULE_EXACT_ALARM denied, then allowed.
@RunWith(AndroidJUnit4::class)
class ReminderAlarmIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val scheduler = AndroidReminderScheduler(context)
    private val slug = "hotfix-" + UUID.randomUUID().toString()
    private val destination = "https://mydailysanctuary.com/prayers/$slug"
    private fun records() = ZonedDateTime.now().plusMinutes(2).let {
        listOf(AccountContentReminder("prayers", slug, "Reminder integration fixture", it.hour, it.minute))
    }
    private fun alarms(): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand("dumpsys alarm")
    ).bufferedReader().use { it.readText() }

    private fun reminderAlarms(): List<String> = alarms().split(Regex("\\n(?=    (?:RTC|ELAPSED))"))
        .filter { it.lineSequence().firstOrNull()?.contains(context.packageName) == true && it.contains("ACTION_SHOW_REMINDER") }

    @Before fun prepare() {
        check(context.packageName.endsWith(".dev")) { "Run only in the disposable DEV test installation" }
        scheduler.cancelAll()
        scheduler.setAccount("local-hotfix-fixture")
    }
    @After fun clean() { scheduler.cancelAll() }

    @Test fun deniedPermissionRetainsSyncedDefinitionWithoutAnAlarm() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        assumeFalse(scheduler.preciseTimingAllowed())
        val records = records()
        scheduler.reconcile(records)
        assertEquals(records.first().hour to records.first().minute, scheduler.itemTime(destination))
        assertTrue(scheduler.needsTimingSetup())
        assertFalse("Denied reminder must not become an inexact alarm", reminderAlarms().isNotEmpty())
        scheduler.resyncItems()
        assertFalse("Resume must not install an approximate alarm", reminderAlarms().isNotEmpty())
        assertTrue(scheduler.needsTimingSetup())
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow"
        )).use { it.readBytes() }
        assertTrue(scheduler.preciseTimingAllowed())
        scheduler.resyncItems()
        assertFalse("Permission recovery must activate the saved definition", scheduler.needsTimingSetup())
        assertEquals(records.first().hour to records.first().minute, scheduler.itemTime(destination))
        assertTrue(reminderAlarms().single().contains("window=0"))
    }

    @Test fun allowedPermissionInstallsExactAlarmAndQuietRefreshPreservesIt() {
        assumeTrue(scheduler.preciseTimingAllowed())
        val records = records()
        scheduler.reconcile(records)
        assertFalse(scheduler.needsTimingSetup())
        val first = reminderAlarms()
        assertEquals("Expected one app reminder alarm", 1, first.size)
        assertTrue("Native reminder must be exact", first.single().contains("window=0"))
        scheduler.reconcile(records)
        assertTrue(reminderAlarms().isNotEmpty())
        scheduler.removeItem(destination)
        assertNull(scheduler.itemTime(destination))
        // Removal is also checked through the app's stored definitions, independent of dump history.
        assertFalse(scheduler.needsTimingSetup())
    }
    @Test fun exactPrayerAndNovenaDeliverDuringForcedIdle() {
        assumeTrue(scheduler.preciseTimingAllowed())
        assumeTrue(scheduler.notificationsAllowed())
        val target = ZonedDateTime.now().plusMinutes(2).withSecond(0).withNano(0)
        val title = "Idle fixture " + UUID.randomUUID().toString()
        val records = listOf(
            AccountContentReminder("prayers", slug, title + " prayer", target.hour, target.minute),
            AccountContentReminder("novenas", slug, title + " novena", target.hour, target.minute)
        )
        fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)
        ).bufferedReader().use { it.readText() }
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        scheduler.reconcile(records)
        assertEquals(2, reminderAlarms().size)
        try {
            shell("dumpsys battery unplug")
            val idle = shell("dumpsys deviceidle force-idle deep")
            assertTrue("Could not enter idle: " + idle, shell("dumpsys deviceidle").contains("mState=IDLE"))
            val deadline = target.toInstant().toEpochMilli() + 10000
            var delivered: List<android.service.notification.StatusBarNotification>
            do {
                Thread.sleep(250)
                delivered = manager.activeNotifications.filter {
                    it.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.startsWith(title) == true
                }
            } while (delivered.size < 2 && System.currentTimeMillis() < deadline)
            assertEquals("Both reminders must deliver while idle", 2, delivered.size)
            delivered.forEach {
                assertTrue("Reminder delivery outside ten-second test tolerance",
                    kotlin.math.abs(it.postTime - target.toInstant().toEpochMilli()) < 10000)
            }
        } finally {
            shell("dumpsys deviceidle unforce")
            shell("dumpsys battery reset")
            manager.cancelAll()
        }
    }

}
