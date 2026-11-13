package app.sanctuary.api.messaging;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
@Component
@EnableScheduling
public class PushDispatcher {
    private final JdbcTemplate db; private final PushGateway gateway;
    public PushDispatcher(JdbcTemplate db,PushGateway gateway){this.db=db;this.gateway=gateway;}
    @Scheduled(fixedDelay=5000,initialDelay=15000) public void dispatch(){
        if(!gateway.enabled())return;
        // Ambiguous interrupted sends are never automatically resent (avoid duplicate alerts).
        db.update("UPDATE push_deliveries SET state='unknown',error_code='INTERRUPTED',updated_at=now() WHERE state='sending' AND updated_at<now()-interval '10 minutes'");
        for(int i=0;i<50;i++){
            var rows=db.queryForList("""
                UPDATE push_deliveries SET state='sending',updated_at=now() WHERE id=(
                SELECT id FROM push_deliveries WHERE state='pending' ORDER BY updated_at,id FOR UPDATE SKIP LOCKED LIMIT 1)
                RETURNING *
                """);
            if(rows.isEmpty())return;
            var delivery=rows.getFirst();
            var targets=db.queryForList("""
                SELECT p.token,m.* FROM push_devices p JOIN push_messages m ON m.id=?
                WHERE p.id=? AND p.user_id=? AND p.enabled AND p.valid AND p.updated_at>now()-interval '60 days'
                """,delivery.get("message_id"),delivery.get("device_id"),delivery.get("recipient_user_id"));
            if(targets.isEmpty()){db.update("UPDATE push_deliveries SET state='skipped',updated_at=now() WHERE id=?",delivery.get("id"));continue;}
            var target=targets.getFirst();
            try{
                var result=gateway.send(target.get("token").toString(),target.get("title").toString(),target.get("body").toString(),target.get("destination").toString(),target.get("id").toString());
                db.update("UPDATE push_deliveries SET state=?,provider_id=?,error_code=?,updated_at=now() WHERE id=?",result.accepted()?"accepted":"failed",result.providerId(),result.code(),delivery.get("id"));
                if(result.invalid())db.update("UPDATE push_devices SET valid=false WHERE id=? AND token=?",delivery.get("device_id"),target.get("token"));
            }catch(Exception e){db.update("UPDATE push_deliveries SET state='unknown',error_code='INTERRUPTED',updated_at=now() WHERE id=?",delivery.get("id"));}
        }
    }
}
