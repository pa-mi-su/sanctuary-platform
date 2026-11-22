package app.sanctuary.android

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import app.sanctuary.android.data.*
import org.junit.Assert.*
import org.junit.Test

// Uses only APIs present in 1.0.39/1.0.40 so it can seed those APKs before a retained-data update.
class ReminderEmptyInstallFixtureTest {
    @Test fun keepGeneralAlertsEnabledWithNoAccountOrSavedReminders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".dev") && BuildConfig.API_BASE_URL == "http://10.0.2.2:18084")
        val repository = SessionRepository(context, SanctuaryApiFactory.create { null })
        repository.logout()
        assertTrue(context.getSharedPreferences("sanctuary_session", Context.MODE_PRIVATE).edit().commit())
        AndroidReminderScheduler(context).cancelAll()
        assertTrue(AndroidPushNotifications.setEnabled(context, true))
        assertTrue(AndroidPushNotifications.allowed(context))
        assertNull(repository.currentSession())
        assertTrue(AndroidReminderScheduler(context).itemReminders().isEmpty())
    }
}
