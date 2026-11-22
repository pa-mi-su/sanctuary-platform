package app.sanctuary.android

internal enum class ReminderSetupIssue(val messageKey: String) {
    Notifications("itemReminder.permission"),
    PreciseTiming("itemReminder.exactRequired"),
    BackgroundRestricted("itemReminder.backgroundRestricted")
}

internal fun reminderSetupIssue(
    hasReminders: Boolean,
    notificationsAllowed: Boolean,
    timingRequired: Boolean,
    backgroundRestricted: Boolean
): ReminderSetupIssue? = when {
    !hasReminders -> null
    !notificationsAllowed -> ReminderSetupIssue.Notifications
    timingRequired -> ReminderSetupIssue.PreciseTiming
    backgroundRestricted -> ReminderSetupIssue.BackgroundRestricted
    else -> null
}
