package app.sanctuary.android

import app.sanctuary.android.data.AnnouncementState
import com.google.gson.Gson

/** Member snapshots affect the signed-out display only; they never become another account's receipts. */
internal class AnnouncementReceiptCache(private val read: (String) -> String?, private val write: (String, String) -> Unit) {
    private val gson = Gson()
    private fun state(key: String): AnnouncementState = runCatching { gson.fromJson(read(key), AnnouncementState::class.java) }
        .getOrNull()?.takeIf { it.readIds != null && it.clearedIds != null && it.clearedBefore.isFinite() } ?: AnnouncementState()
    val guestForUpload get() = state("state")
    val guestForDisplay get() = merge(guestForUpload, state("member-display"))
    val revision get() = read("member-revision")?.toLongOrNull() ?: 0L
    fun rememberMember(value: AnnouncementState, expectedRevision: Long? = null) {
        if (expectedRevision != null && expectedRevision != revision) return
        write("member-display", gson.toJson(value))
        write("member-revision", (revision + 1).toString())
    }
    fun changeGuest(action: String, ids: List<String>, through: Double = 0.0): AnnouncementState {
        write("state", gson.toJson(apply(guestForUpload, action, ids, through)))
        if (action == "restore") {
            write("member-display", gson.toJson(apply(state("member-display"), action, ids, 0.0)))
            write("member-revision", (revision + 1).toString())
        }
        return guestForDisplay
    }
    private fun merge(a: AnnouncementState, b: AnnouncementState) = AnnouncementState((a.readIds+b.readIds).distinct(), (a.clearedIds+b.clearedIds).distinct(), maxOf(a.clearedBefore,b.clearedBefore))
    private fun apply(s: AnnouncementState, action: String, ids: List<String>, through: Double) = when(action) {
        "read" -> s.copy(readIds=(s.readIds+ids).distinct())
        "clear" -> s.copy(clearedIds=(s.clearedIds+ids).distinct())
        "restore" -> s.copy(clearedIds=s.clearedIds.filter { it !in ids })
        "clearAll" -> s.copy(clearedBefore=maxOf(s.clearedBefore,through))
        else -> s
    }
}
