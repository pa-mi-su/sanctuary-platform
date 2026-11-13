package app.sanctuary.api.messaging;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import app.sanctuary.api.auth.service.AuthFlowException;

@Service
public class PushInstallationRegistry {
    private final JdbcTemplate db;
    public PushInstallationRegistry(JdbcTemplate db){this.db=db;}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    @Transactional public Object register(PushDevicesController.Device device,UUID owner,boolean anonymous){
        String secret=device.installationSecret();
        if(anonymous && (secret==null||!secret.matches("[a-zA-Z0-9_-]{43,128}")))throw PushMessages.bad("A valid installation credential is required.");
        if(secret!=null&&!secret.matches("[a-zA-Z0-9_-]{43,128}"))throw PushMessages.bad("Invalid installation credential.");
        db.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,device.installationId().toString());
        var rows=db.queryForList("SELECT user_id,token,installation_secret_hash,registration_version FROM push_devices WHERE installation_id=? FOR UPDATE",device.installationId());
        if(!rows.isEmpty()){
            var row=rows.getFirst();var stored=row.get("installation_secret_hash");
            if(stored!=null && (secret==null||!MessageDigest.isEqual(stored.toString().getBytes(StandardCharsets.UTF_8),hash(secret).getBytes(StandardCharsets.UTF_8))))throw new AuthFlowException(HttpStatus.FORBIDDEN,"Installation verification failed.");
            // Migrating a legacy installation requires its actual FCM token or authenticated ownership, not an ID alone.
            if(stored==null && !Objects.equals(row.get("token"),device.token()) && (anonymous||!Objects.equals(row.get("user_id"),owner)))throw new AuthFlowException(HttpStatus.FORBIDDEN,"Please update your installation registration.");
        }
        long revision=device.revision()==null?0:device.revision();
        if(!rows.isEmpty() && revision<((Number)rows.getFirst().get("registration_version")).longValue())return Map.of("registered",true,"ignored",true);
        if(rows.isEmpty()&&anonymous){
            db.queryForObject("SELECT pg_advisory_xact_lock(724281040)",Object.class);
            db.update("DELETE FROM push_devices WHERE user_id IS NULL AND updated_at<now()-interval '60 days'");
            if(db.queryForObject("SELECT count(*) FROM push_devices WHERE user_id IS NULL",Integer.class)>=10000)throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE,"New registrations are temporarily unavailable.");
        }
        if(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM push_devices WHERE token=? AND installation_id<>?)",Boolean.class,device.token(),device.installationId())))throw new AuthFlowException(HttpStatus.CONFLICT,"This device token belongs to another installation. Refresh notification registration.");
        db.update("""
            INSERT INTO push_devices(user_id,installation_id,installation_secret_hash,token,platform,app_version,enabled,registration_version,device_label)VALUES(?,?,?,?,?,?,?,?,?)
            ON CONFLICT(installation_id)DO UPDATE SET user_id=excluded.user_id,
            installation_secret_hash=COALESCE(excluded.installation_secret_hash,push_devices.installation_secret_hash),
            device_label=excluded.device_label,registration_version=excluded.registration_version,token=excluded.token,platform=excluded.platform,app_version=excluded.app_version,enabled=excluded.enabled,valid=true,updated_at=now()
            """,owner,device.installationId(),secret==null?null:hash(secret),device.token(),device.platform(),device.appVersion(),device.enabled(),revision,device.deviceLabel());
        return Map.of("registered",true);
    }
}
