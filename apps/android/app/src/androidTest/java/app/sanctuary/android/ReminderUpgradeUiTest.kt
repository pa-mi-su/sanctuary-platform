package app.sanctuary.android

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Requires seedPriorRelease on 1.0.38 followed by candidate installation without clearing data.
 * The loopback server returns 503 for /me/reminders throughout the upgrade test.
 */
class ReminderUpgradeUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun retainedRemindersPromptWithoutReminderApiAndRecoverOnResume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL.startsWith("http://10.0.2.2:18084"))
        val scheduler = AndroidReminderScheduler(context)
        assertEquals(2, scheduler.itemReminders().size)
        assertFalse(scheduler.preciseTimingAllowed())
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Allow precise reminders").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isNotEmpty()
        }
        // Exercise the actual Android special-access screen, not an appops grant.
        compose.onNodeWithText("Open phone settings").performClick()
        allowExactRemindersInSystemSettings(scheduler)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(scheduler.preciseTimingAllowed())
        assertFalse(scheduler.needsTimingSetup())
        compose.onNodeWithText("Me", useUnmergedTree = true).performClick()
        for (reminder in scheduler.itemReminders()) {
            val id = reminder.destination
            compose.onNodeWithTag("reminder:$id").performScrollTo()
            try {
                compose.waitUntil(5_000) {
                    compose.onAllNodesWithTag("image:$id", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                }
            } catch (failure: Throwable) {
                File(context.getExternalFilesDir(null), "image-failure.txt").writeText(compose.onRoot(useUnmergedTree = true).printToString())
                compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                    File(context.getExternalFilesDir(null), "image-failure.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                throw failure
            }
            val row = compose.onNodeWithTag("reminder:$id").fetchSemanticsNode().boundsInRoot
            val thumbnail = compose.onNodeWithTag("thumbnail:$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(56f * context.resources.displayMetrics.density, thumbnail.width, 0.5f)
            assertEquals(64f * context.resources.displayMetrics.density, thumbnail.height, 0.5f)
            val image = compose.onNodeWithTag("image:$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals("Image must fill the bounded thumbnail", thumbnail, image)
            val text = compose.onNodeWithTag("text:$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val edit = compose.onNodeWithTag("edit:$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val delete = compose.onNodeWithTag("delete:$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            for (child in listOf(thumbnail, text, edit, delete)) {
                assertTrue("Reminder content must stay within its card: $child / $row",
                    child.left >= row.left && child.right <= row.right && child.top >= row.top && child.bottom <= row.bottom)
            }
            assertTrue("Thumbnail must not overlap text", thumbnail.right <= text.left)
            assertTrue("Text must not overlap controls", (text.right <= edit.left || text.bottom <= edit.top) && (text.right <= delete.left || text.bottom <= delete.top))
            assertFalse("Controls must not overlap", edit.overlaps(delete))
            assertTrue("Title needs readable width", text.width >= 80 * context.resources.displayMetrics.density)
        }
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(context.getExternalFilesDir(null), "reminder-layout.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        assertEquals(15 to 0, scheduler.itemTime("https://mydailysanctuary.com/prayers/qa-prayer"))
        assertEquals(15 to 5, scheduler.itemTime("https://mydailysanctuary.com/novenas/qa-novena"))
    }
}
