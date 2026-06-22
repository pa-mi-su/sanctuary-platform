package app.sanctuary.api.admin.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.admin.dto.AdminAccessUpdateRequest;
import app.sanctuary.api.admin.dto.AdminUserAccessDto;
import app.sanctuary.api.admin.dto.AdminUsersResponse;
import app.sanctuary.api.admin.service.AdminAuthorizationService;
import app.sanctuary.api.admin.service.AdminUserService;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminUserService adminUserService;

    public AdminUserController(
        AdminAuthorizationService adminAuthorizationService,
        AdminUserService adminUserService
    ) {
        this.adminAuthorizationService = adminAuthorizationService;
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUsersResponse list(
        Authentication authentication,
        @RequestParam(defaultValue = "50") int limit
    ) {
        adminAuthorizationService.requireAdmin(CurrentUser.from(authentication));
        return adminUserService.listUsers(limit);
    }

    @GetMapping("/admin-access")
    public List<AdminUserAccessDto> searchAdminAccess(
        Authentication authentication,
        @RequestParam String email,
        @RequestParam(defaultValue = "10") int limit
    ) {
        adminAuthorizationService.requireAdmin(CurrentUser.from(authentication));
        return adminUserService.searchAdminAccess(email, limit);
    }

    @PutMapping("/{userId}/admin-access")
    public AdminUserAccessDto updateAdminAccess(
        Authentication authentication,
        @PathVariable UUID userId,
        @RequestBody AdminAccessUpdateRequest request
    ) {
        UserAccountDto admin = adminAuthorizationService.requireAdmin(CurrentUser.from(authentication));
        return adminUserService.setAdminAccess(admin.id(), userId, request.admin());
    }
}
