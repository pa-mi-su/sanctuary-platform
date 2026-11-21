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
        assertFalse("This scenario requires exact access denied", scheduler.preciseTimingAllowed())
        val records = records()
        scheduler.reconcile(records)
        assertEquals(records.first().hour to records.first().minute, scheduler.itemTime(destination))
        assertTrue(scheduler.needsTimingSetup())
        assertFalse("Denied reminder must not become an inexact alarm", reminderAlarms().isNotEmpty())
        scheduler.resyncItems()
        assertFalse("Resume must not install an approximate alarm", reminderAlarms().isNotEmpty())
        assertTrue(scheduler.needsTimingSetup())
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "appops set --uid ${context.packageName} SCHEDULE_EXACT_ALARM allow"
        )).use { it.readBytes() }
        assertTrue(scheduler.preciseTimingAllowed())
        scheduler.resyncItems()
        assertFalse("Permission recovery must activate the saved definition", scheduler.needsTimingSetup())
        assertEquals(records.first().hour to records.first().minute, scheduler.itemTime(destination))
        assertTrue(reminderAlarms().single().contains("window=0"))
    }

    @Test fun allowedPermissionInstallsExactAlarmAndQuietRefreshPreservesIt() {
        assertTrue("This scenario requires exact access granted", scheduler.preciseTimingAllowed())
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

    @Test fun resumeKeepsDueAlarmAndEditsReplaceIt() {
        assertTrue("This scenario requires exact access granted", scheduler.preciseTimingAllowed())
        var clock = ZonedDateTime.now()
        val native = AndroidReminderScheduler(context) { clock }
        val target = clock.plusMinutes(2)
        val record = AccountContentReminder("prayers", slug, "Queued reminder", target.hour, target.minute)
        native.reconcile(listOf(record))
        val prefs = context.getSharedPreferences("sanctuary_reminders", android.content.Context.MODE_PRIVATE)
        val original = prefs.getLong("scheduled:$destination", 0)
        assertTrue(original > 0)
        // Move the app clock beyond the due time while the alarm is still queued.
        clock = clock.plusMinutes(3)
        native.resyncItems()
        assertEquals("Resume must preserve the queued occurrence", original, prefs.getLong("scheduled:$destination", 0))
        native.reconcile(listOf(record.copy(minute = (record.minute + 10) % 60)))
        assertNotEquals("Editing must replace the previous native alarm", original, prefs.getLong("scheduled:$destination", 0))
        assertEquals(1, reminderAlarms().size)
    }

    @Test fun staleDuplicateAndForeignAccountBroadcastsCannotDeliver() {
        assertTrue("This scenario requires exact access granted", scheduler.preciseTimingAllowed())
        assertTrue("This scenario requires reminder notifications enabled", scheduler.notificationsAllowed())
        var clock = ZonedDateTime.now()
        val native = AndroidReminderScheduler(context) { clock }
        val target = clock.plusMinutes(2)
        val title = "Broadcast fixture " + UUID.randomUUID()
        native.reconcile(listOf(AccountContentReminder("prayers", slug, title, target.hour, target.minute)))
        val prefs = context.getSharedPreferences("sanctuary_reminders", android.content.Context.MODE_PRIVATE)
        val trigger = prefs.getLong("scheduled:$destination", 0)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        try {
            native.receiveItem(destination, "different-account", trigger)
            native.receiveItem(destination, "local-hotfix-fixture", trigger - 1)
            assertFalse(manager.activeNotifications.any { it.tag == destination })
            assertEquals(trigger, prefs.getLong("scheduled:$destination", 0))
            clock = clock.plusMinutes(3)
            native.receiveItem(destination, "local-hotfix-fixture", trigger)
            assertEquals(1, manager.activeNotifications.count { it.tag == destination })
            val next = prefs.getLong("scheduled:$destination", 0)
            assertTrue("Delivery must rearm tomorrow", next > trigger + 23 * 60 * 60 * 1000L)
            manager.cancel(destination, 4402)
            native.receiveItem(destination, "local-hotfix-fixture", trigger)
            assertFalse("Duplicate delivery must be ignored", manager.activeNotifications.any { it.tag == destination })
            assertEquals(next, prefs.getLong("scheduled:$destination", 0))
            native.removeItem(destination)
            native.receiveItem(destination, "local-hotfix-fixture", next)
            assertNull(native.itemTime(destination))
            assertFalse(manager.activeNotifications.any { it.tag == destination })
        } finally { manager.cancel(destination, 4402) }
    }

    @Test fun bootUpdateAndClockChangesReconstructExactAlarms() {
        assertTrue("This scenario requires exact access granted", scheduler.preciseTimingAllowed())
        scheduler.reconcile(records())
        val prefs = context.getSharedPreferences("sanctuary_reminders", android.content.Context.MODE_PRIVATE)
        val receiver = ReminderBootReceiver()
        for (action in listOf(android.content.Intent.ACTION_BOOT_COMPLETED,
            android.content.Intent.ACTION_MY_PACKAGE_REPLACED, android.content.Intent.ACTION_TIME_CHANGED,
            android.content.Intent.ACTION_TIMEZONE_CHANGED, android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            prefs.edit().remove("scheduled:$destination").commit()
            receiver.onReceive(context, android.content.Intent(action))
            assertTrue("Recovery failed for $action", prefs.getLong("scheduled:$destination", 0) > 0)
            assertTrue(reminderAlarms().single().contains("window=0"))
        }
    }
    @Test fun exactPrayerAndNovenaDeliverDuringForcedIdle() {
        assertTrue("This scenario requires exact access granted", scheduler.preciseTimingAllowed())
        assertTrue("This scenario requires reminder notifications enabled", scheduler.notificationsAllowed())
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
