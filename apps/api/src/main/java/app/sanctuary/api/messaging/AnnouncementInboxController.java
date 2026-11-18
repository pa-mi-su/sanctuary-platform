package app.sanctuary.api.messaging;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import app.sanctuary.api.auth.service.AuthFlowException;
import org.springframework.http.HttpStatus;

@RestController
public class AnnouncementInboxController {
 private final JdbcTemplate db;
 public AnnouncementInboxController(JdbcTemplate db){this.db=db;}
 @GetMapping("/announcements") public Object list(@RequestParam(defaultValue="0") int page){
  if(page<0||page>10000)throw new AuthFlowException(HttpStatus.BAD_REQUEST,"Invalid page");
  var rows=db.queryForList("SELECT id,title,body,destination,(extract(epoch from published_at)*1000) AS published_at FROM announcements WHERE withdrawn_at IS NULL ORDER BY published_at DESC,id DESC LIMIT 51 OFFSET ?",page*50);
  return Map.of("items",rows.stream().limit(50).toList(),"hasMore",rows.size()>50);
 }
 @GetMapping("/announcements/{id}") public Object detail(@PathVariable UUID id){
  var rows=db.queryForList("SELECT id,title,body,destination,(extract(epoch from published_at)*1000) AS published_at FROM announcements WHERE id=? AND withdrawn_at IS NULL",id);
  if(rows.isEmpty())throw new AuthFlowException(HttpStatus.NOT_FOUND,"This announcement is no longer available");return rows.getFirst();
 }
 UUID user(Jwt jwt){
  if(jwt==null)throw new AuthFlowException(HttpStatus.UNAUTHORIZED,"Sign in required");
  var ids=db.queryForList("SELECT id FROM users WHERE cognito_sub=?",UUID.class,jwt.getSubject());
  if(ids.isEmpty())throw new AuthFlowException(HttpStatus.NOT_FOUND,"Profile not found");return ids.getFirst();
 }
 @GetMapping("/me/announcements") public Object state(@AuthenticationPrincipal Jwt jwt){
  UUID user=user(jwt);
  var read=db.queryForList("SELECT announcement_id FROM announcement_receipts WHERE user_id=? AND read_at IS NOT NULL",UUID.class,user);
  var clear=db.queryForList("SELECT announcement_id FROM announcement_receipts WHERE user_id=? AND cleared_at IS NOT NULL",UUID.class,user);
  // Every client applies this cutoff before rendering or calculating unread state.
  // Use the immutable profile creation time, never the current login/load time.
  var before=db.queryForList("SELECT extract(epoch from GREATEST(u.created_at,p.cleared_before))*1000 FROM users u LEFT JOIN announcement_inbox_preferences p ON p.user_id=u.id WHERE u.id=?",Double.class,user);
  return Map.of("readIds",read,"clearedIds",clear,"clearedBefore",before.isEmpty()||before.getFirst()==null?0:before.getFirst());
 }
 public record GuestState(List<UUID> readIds,List<UUID> clearedIds,double clearedBefore){}
 @PostMapping("/me/announcements/guest") @Transactional
 public Object mergeGuest(@AuthenticationPrincipal Jwt jwt,@RequestBody GuestState guest){
  UUID user=user(jwt);
  if(guest==null||guest.readIds()==null||guest.clearedIds()==null||guest.readIds().stream().anyMatch(Objects::isNull)||guest.clearedIds().stream().anyMatch(Objects::isNull)||guest.readIds().size()>10000||guest.clearedIds().size()>10000||!Double.isFinite(guest.clearedBefore())||guest.clearedBefore()<0||guest.clearedBefore()>System.currentTimeMillis())
   throw new AuthFlowException(HttpStatus.BAD_REQUEST,"Invalid inbox state");
  for(UUID id:guest.readIds()) db.update("INSERT INTO announcement_receipts(user_id,announcement_id,read_at) SELECT ?,id,now() FROM announcements WHERE id=? ON CONFLICT(user_id,announcement_id) DO UPDATE SET read_at=COALESCE(announcement_receipts.read_at,excluded.read_at)",user,id);
  for(UUID id:guest.clearedIds()) db.update("INSERT INTO announcement_receipts(user_id,announcement_id,cleared_at) SELECT ?,id,now() FROM announcements WHERE id=? ON CONFLICT(user_id,announcement_id) DO UPDATE SET cleared_at=COALESCE(announcement_receipts.cleared_at,excluded.cleared_at)",user,id);
  if(guest.clearedBefore()>0) db.update("INSERT INTO announcement_inbox_preferences(user_id,cleared_before)VALUES(?,to_timestamp(?/1000.0)) ON CONFLICT(user_id)DO UPDATE SET cleared_before=GREATEST(announcement_inbox_preferences.cleared_before,excluded.cleared_before)",user,guest.clearedBefore());
  return state(jwt);
 }
 public record Change(String action,List<UUID> ids,UUID throughId){}
 @PostMapping("/me/announcements") @Transactional public Object change(@AuthenticationPrincipal Jwt jwt,@RequestBody Change change){
  UUID user=user(jwt);
  if(change==null||change.action()==null)throw new AuthFlowException(HttpStatus.BAD_REQUEST,"Choose an action");
  if("clearAll".equals(change.action())){
   var dates=db.queryForList("SELECT published_at FROM announcements WHERE id=? AND withdrawn_at IS NULL",change.throughId());
   if(dates.isEmpty())throw new AuthFlowException(HttpStatus.BAD_REQUEST,"Refresh announcements first");
   db.update("INSERT INTO announcement_inbox_preferences(user_id,cleared_before)VALUES(?,?) ON CONFLICT(user_id)DO UPDATE SET cleared_before=GREATEST(announcement_inbox_preferences.cleared_before,excluded.cleared_before)",user,dates.getFirst().get("published_at"));
  }else{
   if(!Set.of("read","clear","restore").contains(change.action())||change.ids()==null||change.ids().size()>100)throw new AuthFlowException(HttpStatus.BAD_REQUEST,"Invalid action");
   for(UUID id:change.ids()){
    if(!Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM announcements WHERE id=? AND withdrawn_at IS NULL)",Boolean.class,id)))continue;
    if("restore".equals(change.action()))db.update("UPDATE announcement_receipts SET cleared_at=NULL WHERE user_id=? AND announcement_id=?",user,id);
    else if("read".equals(change.action()))db.update("INSERT INTO announcement_receipts(user_id,announcement_id,read_at)VALUES(?,?,now()) ON CONFLICT(user_id,announcement_id)DO UPDATE SET read_at=now()",user,id);
    else db.update("INSERT INTO announcement_receipts(user_id,announcement_id,cleared_at)VALUES(?,?,now()) ON CONFLICT(user_id,announcement_id)DO UPDATE SET cleared_at=now()",user,id);
   }
  }
  return state(jwt);
 }
}
