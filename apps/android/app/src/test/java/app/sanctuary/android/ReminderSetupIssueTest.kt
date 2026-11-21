package app.sanctuary.android

import org.junit.Assert.*
import org.junit.Test

class ReminderSetupIssueTest {
    @Test fun noReminderDoesNotTriggerSetup() {
        assertNull(reminderSetupIssue(false, false, true, true))
    }
    @Test fun notificationsMustBeEnabledBeforeTimingCanHelp() {
        assertEquals(ReminderSetupIssue.Notifications, reminderSetupIssue(true, false, true, true))
    }
    @Test fun preciseAlarmRecoveryPrecedesBackgroundGuidance() {
        assertEquals(ReminderSetupIssue.PreciseTiming, reminderSetupIssue(true, true, true, true))
    }
    @Test fun backgroundRestrictionsAreNotReportedAsHealthy() {
        assertEquals(ReminderSetupIssue.BackgroundRestricted, reminderSetupIssue(true, true, false, true))
    }
    @Test fun fullyEnabledRemindersNeedNoSetup() {
        assertNull(reminderSetupIssue(true, true, false, false))
    }
}
