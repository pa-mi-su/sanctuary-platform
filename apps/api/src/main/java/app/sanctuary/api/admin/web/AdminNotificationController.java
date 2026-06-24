package app.sanctuary.api.admin.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.dto.AdminNotificationSendResultDto;
import app.sanctuary.api.admin.service.AdminAuthorizationService;
import app.sanctuary.api.admin.service.AdminNotificationService;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final AdminAuthorizationService adminAuthorizationService;
    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(
        AdminAuthorizationService adminAuthorizationService,
        AdminNotificationService adminNotificationService
    ) {
        this.adminAuthorizationService = adminAuthorizationService;
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping
    public List<AdminNotificationDto> history(
        Authentication authentication,
        @RequestParam(defaultValue = "50") int limit
    ) {
        adminAuthorizationService.requireAdmin(CurrentUser.from(authentication));
        return adminNotificationService.history(limit);
    }

    @PostMapping("/send")
    public AdminNotificationSendResultDto send(
        Authentication authentication,
        @Valid @RequestBody AdminNotificationRequest request
    ) {
        var admin = adminAuthorizationService.requireAdmin(CurrentUser.from(authentication));
        return adminNotificationService.send(admin.id(), request);
    }
}
