package app.sanctuary.api.news;

import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import app.sanctuary.api.auth.service.WebAuthCookieService;

@Repository
public class NewsMfaSessions {
    public static final String COOKIE="sanctuary_news_mfa";
    private final JdbcTemplate db;
    private final NewsAdminDirectory directory;
    public NewsMfaSessions(JdbcTemplate db, NewsAdminDirectory directory){this.db=db;this.directory=directory;}
    public String issue(org.springframework.security.oauth2.jwt.Jwt identity,int seconds){
        if (seconds <= 0) throw new IllegalArgumentException("Session duration must be positive");
        String username=identity.getClaimAsString("cognito:username");
        if(username==null||username.isBlank())username=identity.getSubject();
        if(username==null||username.isBlank())throw new IllegalArgumentException("Session subject is required");
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);
        String proof=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        db.update("DELETE FROM news_mfa_sessions WHERE expires_at <= now()");
        db.update("INSERT INTO news_mfa_sessions(proof_hash,token_hash,subject,expires_at) VALUES (?,?,?,?)",hash(proof),hash(identity.getTokenValue()),username,OffsetDateTime.now().plusSeconds(Math.min(seconds,3600)));
        return proof;
    }
    public boolean valid(String token,HttpServletRequest request){
        String proof=WebAuthCookieService.cookieValue(request,COOKIE);
        if(proof==null||proof.length()>100)return false;
        var subjects=db.queryForList("SELECT subject FROM news_mfa_sessions WHERE proof_hash=? AND token_hash=? AND expires_at>now()",String.class,hash(proof),hash(token));
        if(subjects.size()!=1)return false;
        if(directory.active(subjects.getFirst()))return true;
        // Revoked access cannot reappear when a role is later re-added. Fail closed on outages too.
        db.update("DELETE FROM news_mfa_sessions WHERE proof_hash=?",hash(proof));
        return false;
    }
    public void revoke(HttpServletRequest request){
        String proof=WebAuthCookieService.cookieValue(request,COOKIE);
        if(proof!=null&&proof.length()<=100)db.update("DELETE FROM news_mfa_sessions WHERE proof_hash=?",hash(proof));
    }
    private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
