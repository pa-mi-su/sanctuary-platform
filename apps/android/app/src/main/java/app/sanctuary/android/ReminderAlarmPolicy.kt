package app.sanctuary.android

// User-selected times must never silently become approximate alarms.
internal fun scheduleExactReminder(
    allowed: Boolean,
    cancel: () -> Unit,
    schedule: () -> Unit
): Boolean {
    if (!allowed) { cancel(); return false }
    return try { schedule(); true }
    catch (_: SecurityException) { cancel(); false } // Permission can change after the check.
}
