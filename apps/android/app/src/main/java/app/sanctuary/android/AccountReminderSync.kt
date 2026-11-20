package app.sanctuary.android

import android.content.Context
import app.sanctuary.android.data.AccountContentReminder
import app.sanctuary.android.data.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// All calls and account transitions run on the main dispatcher. Network I/O is in SessionRepository.
object AccountReminderSync {
    private val mutex = Mutex()
    private var owner: String? = null
    private var generation = 0
    private var refreshingGeneration: Int? = null
    private var repository: SessionRepository? = null
    private var scheduler: AndroidReminderScheduler? = null
    private val _problem = MutableStateFlow<String?>(null)
    val problem = _problem.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun configure(context: Context, repository: SessionRepository, userId: String?) {
        this.repository = repository
        val native = scheduler ?: AndroidReminderScheduler(context.applicationContext).also { scheduler = it }
        if (owner != userId || userId == null) { generation++; _problem.value = null }
        owner = userId
        native.setAccount(userId)
    }

    suspend fun refresh() {
        if (owner == null) return
        val version = generation
        if (refreshingGeneration == version) return
        refreshingGeneration = version
        try { transact(null) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Retry quietly while the reminder list is visible. */ }
        finally { if (refreshingGeneration == version) refreshingGeneration = null }
    }
    suspend fun save(destination: String, title: String, hour: Int, minute: Int) {
        val native = checkNotNull(scheduler)
        check(native.notificationsAllowed()) { "itemReminder.permission" }
        check(native.preciseTimingAllowed()) { "itemReminder.exactRequired" }
        val (kind, slug) = contentKey(destination)
        transact { it.saveContentReminder(AccountContentReminder(kind, slug, title, hour, minute)) }
    }
    suspend fun remove(destination: String) {
        val (kind, slug) = contentKey(destination)
        transact { it.deleteContentReminder(kind, slug) }
    }
    suspend fun clear() { transact { it.clearContentReminders() } }

    private suspend fun transact(mutation: (suspend (SessionRepository) -> Unit)?) {
        val expectedOwner = checkNotNull(owner) { "Please sign in to continue." }
        val version = generation
        mutex.withLock {
            check(owner == expectedOwner && generation == version) { "Your account changed. Please try again." }
            _busy.value = mutation != null
            try {
                val repo = checkNotNull(repository)
                mutation?.invoke(repo)
                val records = repo.contentReminders()
                check(owner == expectedOwner && generation == version) { "Your account changed. Please try again." }
                checkNotNull(scheduler).reconcile(records)
                val native = checkNotNull(scheduler)
                _problem.value = when {
                    records.isNotEmpty() && !native.notificationsAllowed() -> "itemReminder.permission"
                    native.needsTimingSetup() -> "itemReminder.exactRequired"
                    else -> null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (version == generation) _problem.value = "reminders.manage.unavailable"
                throw failure
            } finally { _busy.value = false }
        }
    }

    internal fun contentKey(destination: String): Pair<String, String> {
        val match = Regex("https://mydailysanctuary\\.com/(prayers|novenas)/([A-Za-z0-9_-]+)").matchEntire(destination)
            ?: error("Invalid prayer or novena link.")
        return match.groupValues[1] to match.groupValues[2]
    }
}
