package app.sanctuary.api.messaging;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import app.sanctuary.api.auth.service.AuthFlowException;
@Service
public class PushRegistrationLimiter {
    private final JdbcTemplate db;
    public PushRegistrationLimiter(JdbcTemplate db){this.db=db;}
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void check(String ip){
        long window=java.time.Instant.now().getEpochSecond()/3600;
        db.update("DELETE FROM push_registration_limits WHERE window_start<?",window-2);
        for(String bucket:new String[]{"global",PushInstallationRegistry.hash(ip)}){
            int count=db.queryForObject("INSERT INTO push_registration_limits(bucket,window_start,requests)VALUES(?,?,1)ON CONFLICT(bucket,window_start)DO UPDATE SET requests=push_registration_limits.requests+1 RETURNING requests",Integer.class,bucket,window);
            if(count>(bucket.equals("global")?10000:1000))throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS,"Too many registration requests. Try again later.");
        }
    }
}
