package app.sanctuary.android

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*

internal fun allowExactRemindersInSystemSettings(scheduler: AndroidReminderScheduler) {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    var systemSwitch: AccessibilityNodeInfo? = null
    val settingsDeadline = SystemClock.uptimeMillis() + 10_000
    while (systemSwitch == null && SystemClock.uptimeMillis() < settingsDeadline) {
        val root = automation.rootInActiveWindow
        if (root?.packageName?.toString() == "com.android.settings") {
            fun findSwitch(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                if (node.isCheckable) return node
                for (index in 0 until node.childCount) {
                    node.getChild(index)?.let { child -> findSwitch(child)?.let { return it } }
                }
                return null
            }
            systemSwitch = findSwitch(root)
        }
        if (systemSwitch == null) SystemClock.sleep(50)
    }
    val toggle = checkNotNull(systemSwitch) { "Android Alarms & reminders switch was not displayed" }
    assertFalse(toggle.isChecked)
    var clickTarget = toggle
    while (!clickTarget.isClickable && clickTarget.parent != null) clickTarget = clickTarget.parent
    assertTrue(clickTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    val grantDeadline = SystemClock.uptimeMillis() + 5_000
    while (!scheduler.preciseTimingAllowed() && SystemClock.uptimeMillis() < grantDeadline) SystemClock.sleep(50)
    assertTrue("Grant through the real Settings screen must take effect", scheduler.preciseTimingAllowed())
    assertTrue(automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK))
}
