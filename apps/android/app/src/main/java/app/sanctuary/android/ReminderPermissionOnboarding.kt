package app.sanctuary.android

import android.content.Context
import android.os.Build

// Permission education is installation-wide, independent of login, API data, or saved reminders.
internal class ReminderPermissionOnboarding(context: Context) {
    private val preferences = context.getSharedPreferences("sanctuary_reminder_setup", Context.MODE_PRIVATE)
    private val installation = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
    private val build = "${BuildConfig.VERSION_NAME}:${BuildConfig.VERSION_CODE}"

    fun shouldOffer(preciseTimingAllowed: Boolean): Boolean = shouldOfferReminderPermission(
        supported = Build.VERSION.SDK_INT >= 31,
        allowed = preciseTimingAllowed,
        currentBuild = build,
        currentInstallation = installation,
        answeredBuild = preferences.getString("answered_build", null),
        answeredInstallation = preferences.getLong("answered_installation", 0)
    )

    fun acknowledge() {
        preferences.edit().putString("answered_build", build)
            .putLong("answered_installation", installation).commit()
    }
}

internal fun shouldOfferReminderPermission(
    supported: Boolean,
    allowed: Boolean,
    currentBuild: String,
    currentInstallation: Long,
    answeredBuild: String?,
    answeredInstallation: Long
): Boolean = supported && !allowed &&
    (answeredBuild != currentBuild || answeredInstallation != currentInstallation)
