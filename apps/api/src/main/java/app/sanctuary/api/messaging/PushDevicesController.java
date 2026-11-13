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
    private final PushInstallationRegistry registry;
    public PushDevicesController(JdbcTemplate db, UserAccountService accounts) { this.db=db; this.accounts=accounts; this.registry=new PushInstallationRegistry(db); }
    public record Device(@NotNull UUID installationId, @NotBlank @Size(min=20,max=4096) String token,
        @Pattern(regexp="ios|android") @NotNull String platform, @NotBlank @Size(max=50) String appVersion, boolean enabled, @Size(max=128) String installationSecret, @PositiveOrZero Long revision, @Size(max=100) String deviceLabel) {
        public Device(UUID installationId,String token,String platform,String appVersion,boolean enabled,String secret,Long revision){this(installationId,token,platform,appVersion,enabled,secret,revision,null);}
        public Device(UUID installationId,String token,String platform,String appVersion,boolean enabled,String secret){this(installationId,token,platform,appVersion,enabled,secret,null,null);}
        public Device(UUID installationId,String token,String platform,String appVersion,boolean enabled){this(installationId,token,platform,appVersion,enabled,null,null,null);}
    }
    @PutMapping @Transactional public Object register(Authentication authentication, @Valid @RequestBody Device device) {
        var user = accounts.ensureAccount(CurrentUser.from(authentication));
        return registry.register(device,user.id(),false);
    }
    @DeleteMapping("/{installationId}") public void unregister(Authentication authentication,@PathVariable UUID installationId) {
        var subject = CurrentUser.from(authentication).cognitoSub();
        db.update("UPDATE push_devices SET enabled=false,updated_at=now() WHERE installation_id=? AND user_id IN (SELECT id FROM users WHERE cognito_sub=?)",installationId,subject);
    }
}
