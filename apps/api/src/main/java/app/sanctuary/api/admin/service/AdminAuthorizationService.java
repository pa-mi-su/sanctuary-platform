package app.sanctuary.api.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class AdminAuthorizationService {

    private final UserAccountService userAccountService;
    private final AuthProperties authProperties;
    private final CognitoAuthService cognitoAuthService;

    public AdminAuthorizationService(
        UserAccountService userAccountService,
        AuthProperties authProperties,
        CognitoAuthService cognitoAuthService
    ) {
        this.userAccountService = userAccountService;
        this.authProperties = authProperties;
        this.cognitoAuthService = cognitoAuthService;
    }

    public UserAccountDto requireAdmin(CurrentUser currentUser) {
        UserAccountDto account = userAccountService.ensureAccount(currentUser);
        String adminGroup = authProperties.adminGroup();
        if (!currentUser.belongsToGroup(adminGroup)
            && !cognitoAuthService.isUserInGroup(currentUser.cognitoSub(), currentUser.email(), adminGroup)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required.");
        }
        return account;
    }
}
