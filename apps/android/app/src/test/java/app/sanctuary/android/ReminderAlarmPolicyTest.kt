package app.sanctuary.android

import org.junit.Assert.*
import org.junit.Test

class ReminderAlarmPolicyTest {
    @Test fun missingPermissionCancelsInsteadOfSchedulingApproximately() {
        var cancelled = false
        val result = scheduleExactReminder(false, { cancelled = true }, { fail("Denied permission must not schedule") })
        assertFalse(result)
        assertTrue(cancelled)
    }
    @Test fun grantedPermissionSchedulesExactlyOnce() {
        var calls = 0
        assertTrue(scheduleExactReminder(true, { fail("Granted permission must not cancel") }, { calls++ }))
        assertEquals(1, calls)
    }
    @Test fun permissionRevokedDuringSchedulingCancelsWithoutFallback() {
        var cancelled = false
        assertFalse(scheduleExactReminder(true, { cancelled = true }, { throw SecurityException("fixture revoke") }))
        assertTrue(cancelled)
    }
    @Test(expected = IllegalArgumentException::class) fun unrelatedFailureIsNotHidden() {
        scheduleExactReminder(true, {}, { throw IllegalArgumentException("fixture bad time") })
    }
}
