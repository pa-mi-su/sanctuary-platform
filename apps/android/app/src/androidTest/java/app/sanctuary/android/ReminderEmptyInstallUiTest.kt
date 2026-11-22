package app.sanctuary.android

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Separate fresh installs, retained-data updates with an empty cache, declined setup, and existing grants. */
class ReminderEmptyInstallUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun preciseAlarmSetupIsIndependentOfGeneralAlertsAccountAndReminders() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL == "http://10.0.2.2:18084")
        val scenario = checkNotNull(InstrumentationRegistry.getArguments().getString("scenario"))
        val scheduler = AndroidReminderScheduler(context)
        assertTrue(scheduler.itemReminders().isEmpty())
        if (scenario.startsWith("fresh")) {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Get Sanctuary alerts?").fetchSemanticsNodes().isNotEmpty() }
            if (scenario == "fresh-later") compose.onNodeWithText("Not now").performClick()
            else {
                assertFalse(AndroidPushNotifications.runtimePermissionGranted(context))
                compose.onNodeWithText("Enable alerts").performClick()
                val automation = instrumentation.uiAutomation
                val deadline = SystemClock.uptimeMillis() + 10_000
                fun findAllow(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
                    if (node.isClickable && node.text?.toString() == "Allow") return node
                    for (index in 0 until node.childCount) node.getChild(index)?.let { child ->
                        findAllow(child)?.let { return it }
                    }
                    return null
                }
                var allow: AccessibilityNodeInfo? = null
                while (allow == null && SystemClock.uptimeMillis() < deadline) {
                    automation.rootInActiveWindow?.takeIf { it.packageName?.contains("permissioncontroller") == true }
                        ?.let { allow = findAllow(it) }
                    if (allow == null) SystemClock.sleep(50)
                }
                assertTrue(checkNotNull(allow) { "Android notification Allow button did not open" }
                    .performAction(AccessibilityNodeInfo.ACTION_CLICK))
                compose.waitUntil(5_000) { AndroidPushNotifications.allowed(context) }
            }
        }
        if (scenario == "allowed") {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty() }
            compose.waitForIdle()
            assertTrue(scheduler.preciseTimingAllowed())
            compose.onNodeWithText("Allow precise reminders").assertDoesNotExist()
        } else {
            assertFalse(scheduler.preciseTimingAllowed())
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Allow precise reminders").assertIsDisplayed()
            // Include native window transitions when recording the hand-off from general alerts.
            instrumentation.uiAutomation.waitForIdle(500, 5_000)
            checkNotNull(instrumentation.uiAutomation.takeScreenshot()).let { bitmap ->
                File(context.getExternalFilesDir(null), "reminder-empty-$scenario-prompt.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
            if (scenario == "decline") {
                compose.onNodeWithText("Close").performClick()
                compose.activityRule.scenario.recreate()
                compose.waitForIdle()
                compose.onNodeWithText("Allow precise reminders").assertDoesNotExist()
                openPreciseSettings()
                compose.onNodeWithText("Alarms & reminders: off.", substring = true).assertExists()
                compose.onNodeWithText("Timed prayer & novena reminders").performClick()
            } else compose.onNodeWithText("Open phone settings").performClick()
            allowExactRemindersInSystemSettings(scheduler)
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isEmpty() }
            assertTrue(scheduler.preciseTimingAllowed())
        }
        assertTrue("Setup must not create phantom reminders", scheduler.itemReminders().isEmpty())
        if (scenario == "fresh-later") assertFalse(AndroidPushNotifications.allowed(context))
        else assertTrue(AndroidPushNotifications.allowed(context))
        if (scenario != "decline") openPreciseSettings()
        compose.onNodeWithText("Alarms & reminders: on.", substring = true).assertExists()
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(context.getExternalFilesDir(null), "reminder-empty-$scenario.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun openPreciseSettings() {
        compose.onNodeWithText("Me", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Notifications").performClick()
        compose.onNodeWithText("Timed prayer & novena reminders").performScrollTo().assertIsDisplayed()
    }
}
