package app.sanctuary.android

import android.Manifest
import android.app.AlarmManager
import android.app.ActivityManager
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

class AndroidReminderScheduler(private val context: Context) {
    private var now: () -> ZonedDateTime = { ZonedDateTime.now() }
    internal constructor(context: Context, now: () -> ZonedDateTime) : this(context) { this.now = now }
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun syncDigestReminder(
        activeCommitmentCount: Int,
        novenaEnabled: Boolean,
        generalDailyEnabled: Boolean
    ) {
        // Retire legacy morning/evening digests; per-item reminders are independent.
        preferences.edit().putBoolean(KEY_NOVENA_ENABLED, false).putBoolean(KEY_GENERAL_ENABLED, false).apply()
        cancelLegacyDigests()
    }

    fun resyncFromStoredPreferences() {
        syncDigestReminder(
            activeCommitmentCount = preferences.getInt(KEY_ACTIVE_COUNT, 0),
            novenaEnabled = preferences.getBoolean(KEY_NOVENA_ENABLED, false),
            generalDailyEnabled = preferences.getBoolean(KEY_GENERAL_ENABLED, false)
        )
    }

    fun setAccount(owner: String?) {
        if (preferences.getString("account_owner", null) == null && itemReminders().isNotEmpty()) {
            preferences.edit().putBoolean("legacy_notice", true).commit()
        }
        if (preferences.getString("account_owner", null) != owner || owner == null) {
            clearItemReminders()
            preferences.edit().putString("account_owner", owner).commit()
        }
        // Old, unowned schedules are deliberately not imported into a different account.
    }

    fun reconcile(records: List<app.sanctuary.android.data.AccountContentReminder>) {
        val existing = itemReminders().associateBy { it.destination }
        val desired = records.map { it.destination }.toSet()
        itemReminders().filter { it.destination !in desired }.forEach { removeItem(it.destination) }
        val retryTiming = preferences.getBoolean("timing_required", false)
        var timingRequired = false
        records.forEach { record ->
            val previous = existing[record.destination]
            // Quiet refreshes must not replace an alarm that is about to fire.
            if (!retryTiming && preciseTimingAllowed() && previous?.title == record.title && previous.hour == record.hour && previous.minute == record.minute) return@forEach
            val row = org.json.JSONObject().put("title", record.title).put("hour", record.hour).put("minute", record.minute)
            check(preferences.edit().putString("item:${record.destination}", row.toString()).commit())
            if (!scheduleItem(record.destination, record.title, record.hour, record.minute, force = true)) timingRequired = true
        }
        preferences.edit().putBoolean("timing_required", timingRequired).commit()
    }

    fun resyncItem(destination: String, after: Long = 0) {
        val item = itemReminders().firstOrNull { it.destination == destination } ?: return
        if (!scheduleItem(item.destination, item.title, item.hour, item.minute, force = true, after = after)) preferences.edit().putBoolean("timing_required", true).commit()
    }

    fun legacyNotice(): Boolean = preferences.getBoolean("legacy_notice", false)
    fun dismissLegacyNotice() { preferences.edit().putBoolean("legacy_notice", false).apply() }

    fun preciseTimingAllowed(): Boolean = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
    fun notificationsAllowed(): Boolean = areNotificationsAllowed()
    internal fun setupIssue(): ReminderSetupIssue? = reminderSetupIssue(
        hasReminders = itemReminders().isNotEmpty(),
        notificationsAllowed = notificationsAllowed(),
        timingRequired = needsTimingSetup(),
        backgroundRestricted = backgroundRestricted()
    )
    fun backgroundRestricted(): Boolean = Build.VERSION.SDK_INT >= 28 &&
        context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
    fun notificationSettingsIntent(): Intent {
        createNotificationChannel()
        return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
        } else Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    fun hasOwnedReminders(): Boolean = preferences.getString("account_owner", null) != null && itemReminders().isNotEmpty()
    fun needsTimingSetup(): Boolean = itemReminders().isNotEmpty() && (!preciseTimingAllowed() || preferences.getBoolean("timing_required", false))

    data class ItemReminder(val destination: String, val title: String, val hour: Int, val minute: Int)

    fun itemReminders(): List<ItemReminder> = preferences.all.entries.filter { it.key.startsWith("item:") }.mapNotNull { (key, raw) ->
        runCatching {
            val row = org.json.JSONObject(raw as String)
            ItemReminder(key.removePrefix("item:"), row.getString("title"), row.getInt("hour"), row.getInt("minute"))
        }.getOrNull()
    }.sortedBy { it.title.lowercase() }

    fun observeItems(onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key?.startsWith("item:") == true || key == "timing_required") onChange()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun clearItemReminders() {
        itemReminders().forEach { removeItem(it.destination) }
        preferences.edit().putBoolean("timing_required", false).commit()
    }

    fun itemTime(destination: String): Pair<Int, Int>? {
        val raw = preferences.getString("item:$destination", null) ?: return null
        val row = org.json.JSONObject(raw)
        return row.getInt("hour") to row.getInt("minute")
    }

    fun removeItem(destination: String) {
        preferences.edit().remove("item:$destination").remove("scheduled:$destination").commit()
        val operation = itemIntent(destination, "")
        alarmManager.cancel(operation)
        operation.cancel()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(destination, 4402)
    }

    fun resyncItems(force: Boolean = false) {
        if (preferences.getString("account_owner", null) == null) { setAccount(null); return }
        var timingRequired = false
        itemReminders().forEach { item ->
            if (!scheduleItem(item.destination, item.title, item.hour, item.minute, force)) timingRequired = true
        }
        preferences.edit().putBoolean("timing_required", timingRequired).commit()
    }

    private fun itemIntent(destination: String, title: String, triggerAt: Long = 0): PendingIntent {
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            data = android.net.Uri.parse(destination)
            putExtra(EXTRA_TITLE, title)
            putExtra("itemDestination", destination)
            putExtra("reminderOwner", preferences.getString("account_owner", null))
            putExtra("triggerAt", triggerAt)
        }
        return PendingIntent.getBroadcast(context, 4402, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun scheduleItem(destination: String, title: String, hour: Int, minute: Int, force: Boolean = false, after: Long = 0): Boolean {
        val existing = PendingIntent.getBroadcast(context, 4402,
            Intent(context, ReminderNotificationReceiver::class.java).apply {
                action = ACTION_SHOW_REMINDER
                data = android.net.Uri.parse(destination)
            }, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        // Replacing a due/queued alarm on resume can silently move it to tomorrow.
        // Boot, clock changes and permission grants explicitly force reconstruction.
        if (!force && preciseTimingAllowed() && !preferences.getBoolean("timing_required", false) &&
            preferences.getLong("scheduled:$destination", 0) > 0 && existing != null) return true
        val current = now()
        val reference = if (current.toInstant().toEpochMilli() < after)
            java.time.Instant.ofEpochMilli(after).atZone(current.zone) else current
        val next = nextItemReminderTime(reference, hour, minute)
        val triggerAt = next.toInstant().toEpochMilli()
        val operation = itemIntent(destination, title, triggerAt)
        val scheduled = scheduleExactReminder(
            allowed = preciseTimingAllowed(),
            cancel = {
                alarmManager.cancel(operation)
                preferences.edit().remove("scheduled:$destination").commit()
            },
            schedule = { alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation) }
        )
        if (scheduled) check(preferences.edit().putLong("scheduled:$destination", triggerAt).commit())
        return scheduled
    }

    fun receiveItem(destination: String, expectedOwner: String?, triggerAt: Long) {
        val item = itemReminders().firstOrNull { it.destination == destination } ?: return
        if (expectedOwner == null || expectedOwner != preferences.getString("account_owner", null)) return
        if (triggerAt != preferences.getLong("scheduled:$destination", 0)) return
        // Rearm independently of notification access and before displaying anything.
        // A duplicate or stale broadcast then cannot notify twice or rearm a changed reminder.
        resyncItem(destination, after = triggerAt)
        showItem(destination, item.title, expectedOwner)
    }

    fun showItem(destination: String, title: String, expectedOwner: String?) {
        if (expectedOwner == null || preferences.getString("account_owner", null) != expectedOwner || itemTime(destination) == null || !areNotificationsAllowed()) return
        // An old inexact alarm may still be queued during upgrade. Never deliver it late
        // while exact-alarm access is missing; keep the definition for permission recovery.
        if (!preciseTimingAllowed()) {
            preferences.edit().putBoolean("timing_required", true).commit()
            return
        }
        createNotificationChannel()
        val open = Intent(context, MainActivity::class.java).apply {
            data = android.net.Uri.parse(destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val content = PendingIntent.getActivity(context, 4402, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round).setContentTitle(title)
            .setContentText("Take a moment to pray in Sanctuary.").setAutoCancel(true).setContentIntent(content).build()
        try { NotificationManagerCompat.from(context).notify(destination, 4402, notification) } catch (_: SecurityException) {}
    }

    fun cancelAll() {
        setAccount(null)
        cancelLegacyDigests()
    }

    private fun cancelLegacyDigests() {
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

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked after the check above; skip the reminder safely.
        }
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
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelEnabled = manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled() && channelEnabled
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
        val destination = intent.getStringExtra("itemDestination")
        if (destination != null) {
            scheduler.receiveItem(destination, intent.getStringExtra("reminderOwner"), intent.getLongExtra("triggerAt", 0))
            return
        }
        // Ignore a legacy digest broadcast that was already queued during upgrade.
        scheduler.resyncFromStoredPreferences()
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            AndroidReminderScheduler(context).resyncFromStoredPreferences()
            AndroidReminderScheduler(context).resyncItems(force = true)
        }
    }
}


internal fun nextItemReminderTime(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
    require(hour in 0..23 && minute in 0..59)
    var date = now.toLocalDate()
    repeat(370) {
        val local = date.atTime(hour, minute)
        val offsets = now.zone.rules.getValidOffsets(local)
        // Match iOS: a spring-forward gap moves by the gap (02:30 -> 03:30).
        // During fall-back use the first occurrence, never notify twice that day.
        val candidate = if (offsets.isEmpty()) local.atZone(now.zone)
            else ZonedDateTime.ofLocal(local, now.zone, offsets.first())
        if (candidate.isAfter(now)) return candidate
        date = date.plusDays(1)
    }
    error("No valid reminder date found.")
}
