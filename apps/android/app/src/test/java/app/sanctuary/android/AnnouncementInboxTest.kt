package app.sanctuary.android
import app.sanctuary.android.data.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
class AnnouncementInboxTest {
 private fun row(id:String,time:Double)=InboxAnnouncement(id,"Announcement","Full body","https://mydailysanctuary.com/",time)
 @Test fun clearedEntriesAndOlderPagesStayHiddenWhileNewAnnouncementsRemain() {
  val rows=listOf(row("new",200.0),row("clear-one",150.0),row("old",50.0))
  val state=AnnouncementState(clearedIds=listOf("clear-one"),clearedBefore=100.0)
  assertEquals(listOf("new"),visibleAnnouncements(rows,state).map { it.id })
 }
 @Test fun newAccountBaselineHidesBacklogAndBoundaryButAllowsFutureMessages() {
  val state=AnnouncementState(clearedBefore=200.0)
  assertTrue(visibleAnnouncements(listOf(row("old",100.0),row("boundary",200.0)),state).isEmpty())
  assertEquals(listOf("new"),visibleAnnouncements(listOf(row("new",201.0),row("older-page",50.0)),state).map { it.id })
 }
 @Test fun guestReadAndClearStateSurvivesStorageRoundTrip() {
  val state=AnnouncementState(readIds=listOf("read"),clearedIds=listOf("clear"),clearedBefore=100.0)
  assertEquals(state,Gson().fromJson(Gson().toJson(state),AnnouncementState::class.java))
 }
 @Test fun onlyAssociatedSanctuaryContentHasAnOpenAction() {
  assertFalse(announcementHasContentLink("https://mydailysanctuary.com/"))
  assertFalse(announcementHasContentLink("https://mydailysanctuary.com/announcements/123"))
  assertFalse(announcementHasContentLink("https://untrusted.example/saints/test"))
  assertTrue(announcementHasContentLink("https://mydailysanctuary.com/saints/st-cecilia"))
  assertTrue(announcementHasContentLink("https://mydailysanctuary.com/novenas/st-joseph"))
  assertTrue(announcementHasContentLink("https://mydailysanctuary.com/prayers/our-father"))
 }
}
