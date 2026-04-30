package app.sanctuary.android

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.ZonedDateTime

class AndroidReminderScheduler(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun syncDigestReminder(
        activeCommitmentCount: Int,
        novenaEnabled: Boolean,
        generalDailyEnabled: Boolean
    ) {
        preferences.edit()
            .putInt(KEY_ACTIVE_COUNT, activeCommitmentCount)
            .putBoolean(KEY_NOVENA_ENABLED, novenaEnabled)
            .putBoolean(KEY_GENERAL_ENABLED, generalDailyEnabled)
            .apply()

        cancelAll()

        if (!novenaEnabled && !generalDailyEnabled) return
        if (!areNotificationsAllowed()) return

        createNotificationChannel()

        if (activeCommitmentCount > 0 && novenaEnabled) {
            val body = if (activeCommitmentCount == 1) {
                "You have a novena in progress. Take a calm moment to continue your prayer in Sanctuary."
            } else {
                "You have $activeCommitmentCount novenas in progress. Take a calm moment to continue your prayer in Sanctuary."
            }
            scheduleAlarm(
                kind = KIND_MORNING,
                title = "Continue your novena",
                body = body,
                hour = MORNING_HOUR
            )
            scheduleAlarm(
                kind = KIND_EVENING,
                title = "Continue your novena",
                body = body,
                hour = EVENING_HOUR
            )
            return
        }

        if (generalDailyEnabled) {
            scheduleAlarm(
                kind = KIND_MORNING,
                title = "Your sanctuary is calling",
                body = "Spend a peaceful moment exploring saints, prayer, and perhaps beginning a novena today.",
                hour = MORNING_HOUR
            )
        }
    }

    fun resyncFromStoredPreferences() {
        syncDigestReminder(
            activeCommitmentCount = preferences.getInt(KEY_ACTIVE_COUNT, 0),
            novenaEnabled = preferences.getBoolean(KEY_NOVENA_ENABLED, false),
            generalDailyEnabled = preferences.getBoolean(KEY_GENERAL_ENABLED, false)
        )
    }

    fun cancelAll() {
        alarmManager.cancel(pendingIntentFor(KIND_MORNING, "", ""))
        alarmManager.cancel(pendingIntentFor(KIND_EVENING, "", ""))
    }

    fun showReminderNotification(title: String, body: String) {
        if (!areNotificationsAllowed()) return
        createNotificationChannel()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            99,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleAlarm(kind: String, title: String, body: String, hour: Int) {
        val triggerAt = nextTriggerTimeMillis(hour)
        val pendingIntent = pendingIntentFor(kind, title, body)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun pendingIntentFor(kind: String, title: String, body: String): PendingIntent {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context,
            kind.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerTimeMillis(hour: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }

    private fun areNotificationsAllowed(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sanctuary reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily and novena reminder notifications from Sanctuary."
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "app.sanctuary.android.ACTION_SHOW_REMINDER"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"

        private const val PREFS_NAME = "sanctuary_reminders"
        private const val KEY_ACTIVE_COUNT = "active_commitment_count"
        private const val KEY_NOVENA_ENABLED = "novena_enabled"
        private const val KEY_GENERAL_ENABLED = "general_enabled"
        private const val CHANNEL_ID = "sanctuary-reminders"
        private const val NOTIFICATION_ID = 4401
        private const val KIND_MORNING = "morning"
        private const val KIND_EVENING = "evening"
        private const val MORNING_HOUR = 8
        private const val EVENING_HOUR = 20
    }
}

class ReminderNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = AndroidReminderScheduler(context)
        val title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(AndroidReminderScheduler.EXTRA_BODY).orEmpty()
        scheduler.showReminderNotification(title, body)
        scheduler.resyncFromStoredPreferences()
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            AndroidReminderScheduler(context).resyncFromStoredPreferences()
        }
    }
}
