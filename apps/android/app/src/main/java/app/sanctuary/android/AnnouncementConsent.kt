package app.sanctuary.android

/** Missing is distinct from an explicit opt-out; OS permission alone never grants consent. */
data class AnnouncementConsent(val choice: Boolean?) {
    val enabled get() = choice == true
    val needsInvitation get() = choice == null
    companion object {
        fun restore(saved: Boolean?, recovery: Boolean?) = AnnouncementConsent(saved ?: recovery)
    }
}
