package app.sanctuary.api.admin.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.admin.repository.AdminAuthorizationRepository;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class AdminAuthorizationService {

    private final UserAccountService userAccountService;
    private final AdminAuthorizationRepository adminAuthorizationRepository;

    public AdminAuthorizationService(
        UserAccountService userAccountService,
        AdminAuthorizationRepository adminAuthorizationRepository
    ) {
        this.userAccountService = userAccountService;
        this.adminAuthorizationRepository = adminAuthorizationRepository;
    }

    public UserAccountDto requireAdmin(CurrentUser currentUser) {
        UserAccountDto account = userAccountService.ensureAccount(currentUser);
        if (!adminAuthorizationRepository.isAdmin(account.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access is required.");
        }
        return account;
    }
}
