package app.sanctuary.android

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** No Activity is launched: installation's MY_PACKAGE_REPLACED broadcast must restore alarms. */
class ReminderUpgradeAllowedTest {
    @Test fun grantedUpgradeKeepsExactAlarmsWithoutOpeningTheApp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL == "http://10.0.2.2:18084")
        val scheduler = AndroidReminderScheduler(context)
        assertTrue(scheduler.preciseTimingAllowed())
        assertTrue(scheduler.notificationsAllowed())
        assertEquals(2, scheduler.itemReminders().size)
        val prefs = context.getSharedPreferences("sanctuary_reminders", Context.MODE_PRIVATE)
        for (item in scheduler.itemReminders()) {
            assertTrue("Upgrade receiver must reconstruct each native alarm",
                prefs.getLong("scheduled:${item.destination}", 0) > 0)
        }
        val dump = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("dumpsys alarm")
        ).bufferedReader().use { it.readText() }
        val alarms = dump.split(Regex("\\n(?=    (?:RTC|ELAPSED))"))
            .filter { it.lineSequence().firstOrNull()?.contains(context.packageName) == true && it.contains("ACTION_SHOW_REMINDER") }
        assertEquals("Both preserved reminders must have native alarms", 2, alarms.size)
        assertTrue(alarms.all { it.contains("window=0") })
        assertNull(scheduler.setupIssue())
    }
}
