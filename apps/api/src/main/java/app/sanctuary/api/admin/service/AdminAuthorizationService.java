package app.sanctuary.api.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.admin.config.AdminProperties;
import app.sanctuary.api.admin.repository.AdminAuthorizationRepository;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@Service
@EnableConfigurationProperties(AdminProperties.class)
public class AdminAuthorizationService {

    private final UserAccountService userAccountService;
    private final AdminAuthorizationRepository adminAuthorizationRepository;
    private final AdminProperties adminProperties;

    public AdminAuthorizationService(
        UserAccountService userAccountService,
        AdminAuthorizationRepository adminAuthorizationRepository,
        AdminProperties adminProperties
    ) {
        this.userAccountService = userAccountService;
        this.adminAuthorizationRepository = adminAuthorizationRepository;
        this.adminProperties = adminProperties;
    }

    @Transactional
    public UserAccountDto requireAdmin(CurrentUser currentUser) {
        UserAccountDto account = userAccountService.ensureAccount(currentUser);
        if (adminAuthorizationRepository.isAdmin(account.id())) {
            return account;
        }

        if (adminProperties.isBootstrapAdmin(account.email())) {
            adminAuthorizationRepository.grantAdmin(account.id(), "Bootstrapped from configured admin email.");
            return account;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required.");
    }
}
