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
 @Test fun guestReadAndClearStateSurvivesStorageRoundTrip() {
  val state=AnnouncementState(readIds=listOf("read"),clearedIds=listOf("clear"),clearedBefore=100.0)
  assertEquals(state,Gson().fromJson(Gson().toJson(state),AnnouncementState::class.java))
 }
}
