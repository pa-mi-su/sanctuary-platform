package app.sanctuary.android
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import app.sanctuary.android.data.*
import app.sanctuary.android.ui.sanctuaryStrings
import app.sanctuary.android.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.launch

private fun receiptCache(context: Context): AnnouncementReceiptCache {
 val prefs = context.getSharedPreferences("announcement-inbox-" + BuildConfig.ENVIRONMENT,0)
 return AnnouncementReceiptCache({ prefs.getString(it,null) }, { key,value -> check(prefs.edit().putString(key,value).commit()) })
}
private fun guestState(context: Context) = receiptCache(context).guestForDisplay
private val guestMergeMutex = kotlinx.coroutines.sync.Mutex()
private suspend fun memberState(context: Context, repository: SessionRepository): AnnouncementState {
 guestMergeMutex.lock()
 try {
  val prefs = context.getSharedPreferences("announcement-inbox-" + BuildConfig.ENVIRONMENT,0)
  val owner = checkNotNull(repository.currentSession()).email.lowercase()
  val revision = receiptCache(context).revision
  val pending = "pending:" + owner
  val fresh = prefs.getString("state",null)
  if (fresh != null) {
   val prior = runCatching { Gson().fromJson(prefs.getString(pending,null), AnnouncementState::class.java) }.getOrNull() ?: AnnouncementState()
   val now = receiptCache(context).guestForUpload
   prefs.edit().putString(pending,Gson().toJson(AnnouncementState((prior.readIds+now.readIds).distinct(), (prior.clearedIds+now.clearedIds).distinct(), maxOf(prior.clearedBefore,now.clearedBefore)))).remove("state").commit()
  }
  val guest = runCatching { Gson().fromJson(prefs.getString(pending,null),AnnouncementState::class.java) }.getOrNull() ?: AnnouncementState()
  if (guest.readIds.isNotEmpty() || guest.clearedIds.isNotEmpty() || guest.clearedBefore > 0) {
   val merged = repository.mergeGuestAnnouncements(guest)
   prefs.edit().remove(pending).apply()
   check(repository.currentSession()?.email?.lowercase() == owner)
   receiptCache(context).rememberMember(merged, revision)
   return merged
  }
  return repository.announcementState().also {
   check(repository.currentSession()?.email?.lowercase() == owner)
   receiptCache(context).rememberMember(it, revision)
  }
 } finally { guestMergeMutex.unlock() }
}
internal fun visibleAnnouncements(rows:List<InboxAnnouncement>,state:AnnouncementState)=rows.filter { it.id !in state.clearedIds && it.published_at>state.clearedBefore }
suspend fun announcementUnread(context:Context,authenticated:Boolean):Boolean {
 val api=SanctuaryApiFactory.create { null }; val repository=SessionRepository(context,api)
 val state=if(authenticated) memberState(context,repository) else guestState(context)
 return visibleAnnouncements(api.announcements().items,state).any { it.id !in state.readIds }
}
@Composable fun AnnouncementInboxSheet(initialID:String?=null,authenticated:Boolean,onUnread:(Boolean)->Unit,onOpen:(String)->Unit) {
 val context=LocalContext.current;val l10n=sanctuaryStrings();val scope=rememberCoroutineScope()
 val api=remember { SanctuaryApiFactory.create { null } };val repository=remember { SessionRepository(context,api) }
 var rows by remember { mutableStateOf(emptyList<InboxAnnouncement>()) };var state by remember { mutableStateOf(AnnouncementState()) }
 var forcedID by remember(initialID) { mutableStateOf(initialID) }
 var clearThrough by remember { mutableStateOf<InboxAnnouncement?>(null) }
 var generation by remember { mutableIntStateOf(0) }
 var page by remember { mutableIntStateOf(0) };var more by remember { mutableStateOf(false) };var busy by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf("") };var undo by remember { mutableStateOf<String?>(null) };var confirm by remember { mutableStateOf(false) }
 val displayed=visibleAnnouncements(rows,state).let { visible -> rows.firstOrNull { it.id==forcedID && visible.none { row->row.id==it.id } }?.let { listOf(it)+visible } ?: visible }
 fun badge(){onUnread(visibleAnnouncements(rows,state).any { it.id !in state.readIds })}
 fun load(next: Int = 0) {
  generation++
  val request = generation
  val member = authenticated
  scope.launch {
   busy = true
   error = ""
   try {
    if (next == 0) {
     val loaded = if (member) memberState(context,repository) else guestState(context)
     if (request != generation) return@launch
     state = loaded
    }
    val feed = api.announcements(next)
    val requested = if (next == 0 && !initialID.isNullOrBlank() && feed.items.none { it.id == initialID }) api.announcement(initialID) else null
    if (request != generation) return@launch
    rows = if (next == 0) feed.items else (rows + feed.items).distinctBy { it.id }
    if (requested != null) rows = listOf(requested) + rows
    page = next
    more = feed.hasMore
    val unread = (visibleAnnouncements(rows, state).map { it.id } + listOfNotNull(initialID)).distinct().filter { it !in state.readIds }.take(100)
    if (unread.isNotEmpty()) {
     if (member) {
      val revision = receiptCache(context).revision
      val loaded = repository.changeAnnouncements(AnnouncementChange("read", unread))
      if (request != generation) return@launch
      state = loaded
      receiptCache(context).rememberMember(loaded, revision)
     } else {
      state = receiptCache(context).changeGuest("read", unread)
     }
    }
    badge()
    AndroidPushNotifications.acknowledgeAnnouncementAlerts(context)
   } catch (e: Exception) {
    if (request == generation) error = e.message ?: "Could not load announcements"
   } finally {
    if (request == generation) busy = false
   }
  }
 }
 fun change(action:String,id:String?=null,after:()->Unit={}) { if(busy)return;val request=generation;val through=clearThrough ?: rows.maxByOrNull { it.published_at };scope.launch { busy=true;error="";try {
  if(authenticated) { val loaded=repository.changeAnnouncements(AnnouncementChange(action,id?.let { listOf(it) } ?: emptyList(),if(action=="clearAll")through?.id else null));if(request!=generation)return@launch;state=loaded;receiptCache(context).rememberMember(loaded) }
  else { state=receiptCache(context).changeGuest(action,listOfNotNull(id),through?.published_at ?: 0.0) }
  if(action=="clear" || action=="clearAll") AndroidPushNotifications.acknowledgeAnnouncementAlerts(context)
  undo=if(action=="clear")id else null;badge()
 }catch(e:Exception){error=e.message ?: "Could not update inbox"}finally{if(request==generation){busy=false;after()}} } }
 LaunchedEffect(authenticated){confirm=false;clearThrough=null;undo=null;load()}
 Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
 Column(Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
  Text(l10n.t("inbox.title"), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
   InboxAction(l10n.t("inbox.refresh"), Modifier.weight(1f), !busy) { load() }
   InboxAction(l10n.t("inbox.clearAll"), Modifier.weight(1f), !busy && visibleAnnouncements(rows,state).isNotEmpty()) { clearThrough=rows.maxByOrNull { it.published_at };confirm=true }
  }
  if(busy)CircularProgressIndicator()
  if(error.isNotEmpty())Text(error,color=Color(0xFFFFB4AB))
  undo?.let { id -> InboxAction(l10n.t("inbox.undo"), enabled = !busy) { change("restore",id) } }
  if(!busy && displayed.isEmpty())Text(l10n.t("inbox.empty"),color=Color.White,modifier=Modifier.fillMaxWidth().padding(20.dp),textAlign=TextAlign.Center)
  displayed.forEach { row ->
   Card(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=SanctuaryCard),shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,Color(0x997CC7DE))) {
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
     Text(row.title,color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
     Text(java.text.DateFormat.getDateInstance().format(java.util.Date(row.published_at.toLong())),color=Color(0xFFB8CCD6),fontSize=14.sp)
     Text(row.body,color=Color.White,fontSize=17.sp,lineHeight=24.sp)
     HorizontalDivider(color=Color(0x667CC7DE))
     Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      if(announcementHasContentLink(row.destination)) {
       InboxAction(l10n.t("inbox.open"),Modifier.weight(1f),!busy,primary=true) { onOpen(row.destination) }
      }
      InboxAction(l10n.t("inbox.clear"),Modifier.weight(1f),!busy) { forcedID=null;change("clear",row.id) }
     }
    }
   }
  }
  if(more)InboxAction(l10n.t("inbox.more"),enabled=!busy){load(page+1)}
 }
 }

 if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text(l10n.t("inbox.confirmClear"))},confirmButton={TextButton(onClick={confirm=false;forcedID=null;change("clearAll")}){Text(l10n.t("inbox.clearAll"))}},dismissButton={TextButton(onClick={confirm=false}){Text(l10n.t("common.cancel"))}})
}

internal fun announcementHasContentLink(value: String): Boolean = runCatching {
 val uri = java.net.URI(value)
 val parts = uri.path.trim('/').split('/')
 uri.scheme == "https" && (uri.host == "mydailysanctuary.com" || (BuildConfig.ENVIRONMENT == "dev" && uri.host == "dev.mydailysanctuary.com")) && parts.size == 2 && parts[0] in listOf("saints", "prayers", "novenas") && parts[1].isNotBlank()
}.getOrDefault(false)

@Composable
private fun InboxAction(title: String, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = false, action: () -> Unit) {
 OutlinedButton(onClick=action,enabled=enabled,modifier=modifier.fillMaxWidth().heightIn(min=48.dp).background(Brush.linearGradient(if(primary)listOf(Color(0xFF3E9FC1),Color(0xFF195E78)) else listOf(Color(0xFF2A4153),Color(0xFF2A4153))),RoundedCornerShape(20.dp)),shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,Color(0x997CC7DE)),colors=ButtonDefaults.outlinedButtonColors(containerColor=Color.Transparent,contentColor=Color.White)) {
  Text(title,fontSize=16.sp,fontWeight=FontWeight.SemiBold)
 }
}
