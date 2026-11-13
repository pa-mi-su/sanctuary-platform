package app.sanctuary.api.messaging;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.sanctuary.api.auth.service.AuthFlowException;

@Service
public class PushMessages {
    private final JdbcTemplate db;
    private final PushGateway gateway;
    private final String siteUrl;
    public PushMessages(JdbcTemplate db, PushGateway gateway) { this(db,gateway,"https://mydailysanctuary.com"); }
    @org.springframework.beans.factory.annotation.Autowired
    public PushMessages(JdbcTemplate db, PushGateway gateway,
        @org.springframework.beans.factory.annotation.Value("${sanctuary.push.site-url:https://mydailysanctuary.com}") String siteUrl) {
        if (!Set.of("https://mydailysanctuary.com","https://dev.mydailysanctuary.com").contains(siteUrl)) throw new IllegalArgumentException("Invalid notification site URL");
        this.db=db; this.gateway=gateway; this.siteUrl=siteUrl;
    }
    public record Draft(String title,String body,String kind,String contentId,String audience,UUID testDevice) {}
    public record Send(UUID id, Draft draft,String recipientVersion) {}
    static AuthFlowException bad(String text) { return new AuthFlowException(HttpStatus.BAD_REQUEST,text); }
    String destination(Draft d) {
        if (d == null || d.title()==null || d.title().isBlank() || d.title().length()>100 || d.body()==null || d.body().isBlank() || d.body().length()>500) throw bad("Enter a title up to 100 characters and a message up to 500 characters.");
        if (!Set.of("all","ios","android","test","guests","members").contains(Objects.toString(d.audience(),""))) throw bad("Choose an audience.");
        if ("home".equals(d.kind())) return siteUrl+"/";
        if (!Set.of("prayers","saints","novenas").contains(Objects.toString(d.kind(),""))) throw bad("Choose a content destination.");
        var rows=db.queryForList("SELECT slug FROM "+d.kind()+" WHERE id=?",d.contentId());
        if(rows.isEmpty()) throw bad("This content no longer exists. Choose another destination.");
        return siteUrl+"/"+d.kind()+"/"+rows.getFirst().get("slug");
    }
    List<Map<String,Object>> recipients(Draft d) {
        String filter=switch(d.audience()) {
            case "test" -> "p.id::text=? AND EXISTS(SELECT 1 FROM users u JOIN admin_user_directory a USING(cognito_sub) WHERE u.id=p.user_id AND lower(a.email)='pmsulikowski@icloud.com' AND a.email_verified AND a.enabled)";
            case "ios","android" -> "p.platform=?";
            case "guests" -> "p.user_id IS NULL AND ?='guests'";
            case "members" -> "p.user_id IS NOT NULL AND ?='members'";
            default -> "?='all'";
        };
        Object argument="test".equals(d.audience())?Objects.toString(d.testDevice(),""):d.audience();
        return db.queryForList("SELECT p.id,p.user_id,p.token,p.updated_at,p.platform,p.app_version,p.device_label,u.email,u.display_name FROM push_devices p LEFT JOIN users u ON u.id=p.user_id WHERE p.enabled AND p.valid AND p.updated_at>now()-interval '60 days' AND "+filter+" ORDER BY p.id",argument);
    }
    String version(List<Map<String,Object>> rows) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rows.toString().getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    public Object preview(Draft draft) {
        String link=destination(draft); var devices=recipients(draft);
        var details=devices.stream().map(device -> {
            Map<String,Object> detail=new LinkedHashMap<>();
            for(String key:List.of("id","platform","app_version","updated_at","email","display_name","device_label")) detail.put(key,device.get(key));
            detail.put("signedIn",device.get("user_id")!=null);
            return detail;
        }).toList();
        return Map.of("count",devices.size(),"recipientVersion",version(devices),"destination",link,"enabled",gateway.enabled(),"devices",details);
    }
    @Transactional public Object send(String actor,Send request) {
        if(request==null || request.id()==null) throw bad("A send identifier is required.");
        db.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,request.id().toString());
        if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM push_messages WHERE id=?)",Boolean.class,request.id()))) return Map.of("id",request.id(),"queued",true);
        if(!gateway.enabled()) throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,"Messaging is not enabled in this environment.");
        var draft=request.draft(); String destination=destination(draft); var devices=recipients(draft);
        if(devices.isEmpty()) throw bad("No eligible opted-in devices. Update the app and enable announcements first.");
        if(!version(devices).equals(request.recipientVersion())) throw new AuthFlowException(HttpStatus.CONFLICT,"The audience changed. Review the recipients again.");
        db.queryForObject("SELECT pg_advisory_xact_lock(724281039)",Object.class);
        int recent=db.queryForObject("SELECT count(*) FROM push_messages WHERE created_at>now()-interval '1 hour'",Integer.class);
        if(recent>=20) throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS,"The hourly sending limit has been reached.");
        db.update("INSERT INTO push_messages(id,actor_sub,title,body,destination,audience)VALUES(?,?,?,?,?,?)",request.id(),actor,draft.title().strip(),draft.body().strip(),destination,draft.audience());
        for(var device:devices) db.update("INSERT INTO push_deliveries(message_id,device_id,recipient_user_id)VALUES(?,?,?)",request.id(),device.get("id"),device.get("user_id"));
        return Map.of("id",request.id(),"queued",true);
    }
    public Object deliveryDetails(UUID messageId) {
        var devices=db.queryForList("""
            SELECT d.id,d.device_id,d.state,d.error_code,d.updated_at,p.platform,p.app_version,p.device_label,u.email,u.display_name
            FROM push_deliveries d LEFT JOIN push_devices p ON p.id=d.device_id
            LEFT JOIN users u ON u.id=d.recipient_user_id
            WHERE d.message_id=? ORDER BY d.id
            """,messageId);
        return Map.of("devices",devices);
    }
    public Object retireDevice(UUID deviceId) {
        int changed=db.update("UPDATE push_devices SET enabled=false,valid=false,updated_at=now() WHERE id=?",deviceId);
        if(changed==0) throw bad("This registration no longer exists.");
        return Map.of("retired",true);
    }
    public Object overview(int page) {
        if(page<0||page>10000) throw bad("Invalid page.");
        var items=db.queryForList("""
            SELECT m.*,count(d.id) AS recipients,
            count(d.id)FILTER(WHERE d.state='accepted') AS accepted,
            count(d.id)FILTER(WHERE d.state='failed') AS failed,
            count(d.id)FILTER(WHERE d.state IN ('pending','sending')) AS pending,
            count(d.id)FILTER(WHERE d.state='unknown') AS unknown,
            count(d.id)FILTER(WHERE d.state='skipped') AS skipped,
            string_agg(DISTINCT d.error_code,', ') AS errors
            FROM push_messages m LEFT JOIN push_deliveries d ON d.message_id=m.id
            GROUP BY m.id ORDER BY m.created_at DESC,m.id LIMIT 21 OFFSET ?
            """,page*20);
        var tests=db.queryForList("""
            SELECT p.id,p.platform,p.app_version,p.updated_at FROM push_devices p JOIN users u ON u.id=p.user_id
            JOIN admin_user_directory a ON a.cognito_sub=u.cognito_sub
            WHERE lower(a.email)='pmsulikowski@icloud.com' AND a.email_verified AND a.enabled AND p.enabled AND p.valid
            AND p.updated_at>now()-interval '60 days' ORDER BY p.updated_at DESC
            """);
        return Map.of("items",items.stream().limit(20).toList(),"hasMore",items.size()>20,"testDevices",tests,"enabled",gateway.enabled());
    }
}
