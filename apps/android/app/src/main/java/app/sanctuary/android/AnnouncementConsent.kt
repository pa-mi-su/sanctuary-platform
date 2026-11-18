package app.sanctuary.android

/** Missing is distinct from an explicit opt-out; OS permission alone never grants consent. */
data class AnnouncementConsent(val choice: Boolean?, val version: String? = null, val answeredVersion: String? = null) {
    val enabled get() = choice == true
    val needsInvitation get() = choice == null || (choice == false && version != null && version != answeredVersion)
    companion object {
        fun restore(saved: Boolean?, recovery: Boolean?, version: String? = null, answeredVersion: String? = null) = AnnouncementConsent(saved ?: recovery, version, answeredVersion)
    }
}
