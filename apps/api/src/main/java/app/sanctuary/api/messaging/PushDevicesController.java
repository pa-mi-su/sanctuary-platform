package app.sanctuary.api.messaging;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
@RequestMapping("/me/push-devices")
public class PushDevicesController {
    private final JdbcTemplate db;
    private final UserAccountService accounts;
    public PushDevicesController(JdbcTemplate db, UserAccountService accounts) { this.db=db; this.accounts=accounts; }
    public record Device(@NotNull UUID installationId, @NotBlank @Size(min=20,max=4096) String token,
        @Pattern(regexp="ios|android") @NotNull String platform, @NotBlank @Size(max=50) String appVersion, boolean enabled) {}
    @PutMapping @Transactional public Object register(Authentication authentication, @Valid @RequestBody Device device) {
        var user = accounts.ensureAccount(CurrentUser.from(authentication));
        // Token is a device credential; re-registration transfers it to the currently authenticated account.
        db.update("DELETE FROM push_devices WHERE token=? AND installation_id<>?",device.token(),device.installationId());
        db.update("""
            INSERT INTO push_devices(user_id,installation_id,token,platform,app_version,enabled) VALUES (?,?,?,?,?,?)
            ON CONFLICT(installation_id) DO UPDATE SET user_id=excluded.user_id,token=excluded.token,platform=excluded.platform,
            app_version=excluded.app_version,enabled=excluded.enabled,valid=true,updated_at=now()
            """,user.id(),device.installationId(),device.token(),device.platform(),device.appVersion(),device.enabled());
        return Map.of("registered",true);
    }
    @DeleteMapping("/{installationId}") public void unregister(Authentication authentication,@PathVariable UUID installationId) {
        var subject = CurrentUser.from(authentication).cognitoSub();
        db.update("UPDATE push_devices SET enabled=false,updated_at=now() WHERE installation_id=? AND user_id IN (SELECT id FROM users WHERE cognito_sub=?)",installationId,subject);
    }
}
