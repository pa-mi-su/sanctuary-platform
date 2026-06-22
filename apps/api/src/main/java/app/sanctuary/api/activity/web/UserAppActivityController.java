package app.sanctuary.api.activity.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.activity.dto.UserAppActivityRequest;
import app.sanctuary.api.activity.service.UserAppActivityService;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
@RequestMapping("/me/activity")
public class UserAppActivityController {

    private final UserAccountService userAccountService;
    private final UserAppActivityService activityService;

    public UserAppActivityController(
        UserAccountService userAccountService,
        UserAppActivityService activityService
    ) {
        this.userAccountService = userAccountService;
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<Void> record(
        Authentication authentication,
        @Valid @RequestBody UserAppActivityRequest request
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        activityService.record(account.id(), request);
        return ResponseEntity.noContent().build();
    }
}
