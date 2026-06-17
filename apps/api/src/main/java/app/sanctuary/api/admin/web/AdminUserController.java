package app.sanctuary.api.admin.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.admin.dto.AdminUsersResponse;
import app.sanctuary.api.admin.service.AdminAuthorizationService;
import app.sanctuary.api.admin.service.AdminUserService;
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
}
