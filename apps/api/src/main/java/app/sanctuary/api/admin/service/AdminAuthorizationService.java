package app.sanctuary.api.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class AdminAuthorizationService {

    private final UserAccountService userAccountService;
    private final AuthProperties authProperties;

    public AdminAuthorizationService(
        UserAccountService userAccountService,
        AuthProperties authProperties
    ) {
        this.userAccountService = userAccountService;
        this.authProperties = authProperties;
    }

    public UserAccountDto requireAdmin(CurrentUser currentUser) {
        UserAccountDto account = userAccountService.ensureAccount(currentUser);
        if (!currentUser.belongsToGroup(authProperties.adminGroup())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required.");
        }
        return account;
    }
}
