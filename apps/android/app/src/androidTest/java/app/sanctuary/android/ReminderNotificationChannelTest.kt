package app.sanctuary.android

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.AccountContentReminder
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class ReminderNotificationChannelTest {
    @Test fun disabledReminderChannelIsDetectedAndSettingsCanRestoreIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".dev"))
        val scheduler = AndroidReminderScheduler(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val destination = "https://mydailysanctuary.com/prayers/channel-fixture"
        val channelId = "sanctuary-reminders"
        val automation = instrumentation.uiAutomation
        assertTrue(manager.areNotificationsEnabled())
        assertTrue(scheduler.preciseTimingAllowed())
        scheduler.cancelAll()
        scheduler.setAccount("channel-fixture")
        val target = ZonedDateTime.now().plusMinutes(2)
        scheduler.reconcile(listOf(AccountContentReminder("prayers", "channel-fixture", "Channel test", target.hour, target.minute)))
        manager.createNotificationChannel(NotificationChannel(channelId, "Sanctuary reminders", NotificationManager.IMPORTANCE_DEFAULT))
        // Apps may lower channel importance. Raising it again requires a user action.
        manager.createNotificationChannel(NotificationChannel(channelId, "Sanctuary reminders", NotificationManager.IMPORTANCE_NONE))
        try {
            assertTrue("Global app notifications remain enabled", manager.areNotificationsEnabled())
            assertFalse(scheduler.notificationsAllowed())
            assertEquals(ReminderSetupIssue.Notifications, scheduler.setupIssue())
            val prefs = context.getSharedPreferences("sanctuary_reminders", android.content.Context.MODE_PRIVATE)
            val original = prefs.getLong("scheduled:$destination", 0)
            scheduler.receiveItem(destination, "channel-fixture", original)
            assertTrue("Blocked notifications must still rearm the next day", prefs.getLong("scheduled:$destination", 0) > original)
            assertFalse(manager.activeNotifications.any { it.tag == destination })
            instrumentation.runOnMainSync {
                context.startActivity(scheduler.notificationSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            var toggle: AccessibilityNodeInfo? = null
            val deadline = SystemClock.uptimeMillis() + 10_000
            fun findSwitch(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.isCheckable) return node
                for (index in 0 until node.childCount) {
                    node.getChild(index)?.let { child -> findSwitch(child)?.let { return it } }
                }
                return null
            }
            while (toggle == null && SystemClock.uptimeMillis() < deadline) {
                automation.rootInActiveWindow?.takeIf { it.packageName?.toString() == "com.android.settings" }
                    ?.let { toggle = findSwitch(it) }
                if (toggle == null) SystemClock.sleep(50)
            }
            val settingsSwitch = checkNotNull(toggle) { "Reminder notification category settings did not open" }
            assertFalse(settingsSwitch.isChecked)
            var clickTarget = settingsSwitch
            while (!clickTarget.isClickable && clickTarget.parent != null) clickTarget = clickTarget.parent
            assertTrue(clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            val recoveryDeadline = SystemClock.uptimeMillis() + 5_000
            while (!scheduler.notificationsAllowed() && SystemClock.uptimeMillis() < recoveryDeadline) SystemClock.sleep(50)
            assertTrue("The real channel settings switch must restore reminders", scheduler.notificationsAllowed())
            assertNull(scheduler.setupIssue())
        } finally {
            automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            scheduler.cancelAll()
        }
    }
}
