package app.sanctuary.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Seed the old release, install the candidate with -r, then set /fixture/auth to 503 before running. */
class ReminderUpgradeOfflineUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun authenticationOutageDoesNotEraseUpgradeRemindersAndResumeRecovers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL == "http://10.0.2.2:18084")
        val scheduler = AndroidReminderScheduler(context)
        val repository = SessionRepository(context, SanctuaryApiFactory.create { null })
        val model = ViewModelProvider(compose.activity)[MainViewModel::class.java]
        try {
            compose.waitUntil(20_000) { model.session.value.status == SessionStatus.Failed && !model.session.value.isBootstrapping }
            assertNotNull(repository.currentSession())
            assertEquals(2, scheduler.itemReminders().size)
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Allow precise reminders").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Allow precise reminders").assertIsDisplayed()
            compose.onNodeWithText("Open phone settings").performClick()
            allowExactRemindersInSystemSettings(scheduler)
            compose.waitUntil(15_000) { model.session.value.status == SessionStatus.Failed && !scheduler.needsTimingSetup() }
            assertTrue(scheduler.preciseTimingAllowed())
            assertFalse(scheduler.needsTimingSetup())
            assertNotNull(repository.currentSession())
            assertEquals(2, scheduler.itemReminders().size)
            ReminderFixture.auth(200)
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitUntil(15_000) { model.session.value.status == SessionStatus.Authenticated }
            assertEquals(2, scheduler.itemReminders().size)
            // Actual credential rejection still removes account-owned schedules.
            ReminderFixture.auth(401)
            compose.runOnUiThread { model.bootstrap() }
            compose.waitUntil(15_000) { model.session.value.status == SessionStatus.SignedOut }
            assertNull(repository.currentSession())
            assertTrue(scheduler.itemReminders().isEmpty())
        } finally { ReminderFixture.auth(200) }
    }
}
