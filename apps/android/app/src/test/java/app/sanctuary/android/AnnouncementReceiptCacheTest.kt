package app.sanctuary.android
import app.sanctuary.android.data.AnnouncementState
import org.junit.Assert.*
import org.junit.Test
class AnnouncementReceiptCacheTest {
 private val storage=mutableMapOf<String,String>()
 private fun cache()=AnnouncementReceiptCache({storage[it]},{key,value->storage[key]=value})
 @Test fun guestToMemberClearThenLogoutSurvivesRecreation() {
  cache().changeGuest("clear",listOf("guest-old"))
  val guest=cache().guestForUpload
  cache().rememberMember(guest.copy(clearedIds=guest.clearedIds+"member-old"))
  storage.remove("state") // Claimed by sign-in; display state must survive.
  assertEquals(setOf("guest-old","member-old"),cache().guestForDisplay.clearedIds.toSet())
  assertEquals(AnnouncementState(),cache().guestForUpload)
  cache().rememberMember(AnnouncementState(clearedBefore=200.0))
  assertEquals(200.0,cache().guestForDisplay.clearedBefore,0.0)
  assertTrue(201.0>cache().guestForDisplay.clearedBefore)
 }
 @Test fun accountSnapshotsNeverBecomeGuestUploadsForAnotherAccount() {
  cache().rememberMember(AnnouncementState(readIds=listOf("member-read"),clearedIds=listOf("member-clear"),clearedBefore=200.0))
  cache().changeGuest("clear",listOf("guest-new"))
  assertEquals(AnnouncementState(clearedIds=listOf("guest-new")),cache().guestForUpload)
  cache().rememberMember(AnnouncementState()) // Other account's server state.
  assertFalse(cache().guestForDisplay.clearedIds.contains("member-clear"))
  assertEquals(0.0,cache().guestForDisplay.clearedBefore,0.0)
 }
 @Test fun delayedReadCannotReplaceANewerClear() {
  val revision=cache().revision
  cache().rememberMember(AnnouncementState(clearedBefore=200.0))
  cache().rememberMember(AnnouncementState(),revision)
  assertEquals(200.0,cache().guestForDisplay.clearedBefore,0.0)
 }
 @Test fun guestUndoRemovesBothLocalDisplayAndGuestClear() {
  cache().rememberMember(AnnouncementState(clearedIds=listOf("old")))
  assertFalse(cache().changeGuest("restore",listOf("old")).clearedIds.contains("old"))
  cache().changeGuest("clear",listOf("new"))
  assertFalse(cache().changeGuest("restore",listOf("new")).clearedIds.contains("new"))
 }
}
