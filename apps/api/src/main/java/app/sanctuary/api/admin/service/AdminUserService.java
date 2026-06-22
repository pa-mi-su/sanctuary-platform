package app.sanctuary.api.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.admin.dto.AdminUserAccessDto;
import app.sanctuary.api.admin.dto.AdminUsersResponse;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminUserRepository;

@Service
public class AdminUserService {

    private static final int MAX_LIMIT = 250;
    private static final int MAX_ADMIN_ACCESS_SEARCH_LIMIT = 25;

    private final AdminUserRepository repository;
    private final AdminAuditRepository auditRepository;

    public AdminUserService(
        AdminUserRepository repository,
        AdminAuditRepository auditRepository
    ) {
        this.repository = repository;
        this.auditRepository = auditRepository;
    }

    public AdminUsersResponse listUsers(int requestedLimit) {
        int limit = requestedLimit <= 0 ? 50 : Math.min(requestedLimit, MAX_LIMIT);
        return new AdminUsersResponse(repository.metrics(), repository.listUsers(limit), repository.listRecentDeviceInstalls(limit));
    }

    public List<AdminUserAccessDto> searchAdminAccess(String emailQuery, int requestedLimit) {
        String query = emailQuery == null ? "" : emailQuery.trim();
        if (query.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search must include at least 3 characters.");
        }

        int limit = requestedLimit <= 0 ? 10 : Math.min(requestedLimit, MAX_ADMIN_ACCESS_SEARCH_LIMIT);
        return repository.searchAdminAccessByEmail(query, limit);
    }

    @Transactional
    public AdminUserAccessDto setAdminAccess(UUID actorUserId, UUID targetUserId, boolean enabled) {
        AdminUserAccessDto target = repository.findAdminAccessByUserId(targetUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found."));

        if (!enabled && actorUserId.equals(target.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot remove your own admin access.");
        }

        AdminUserAccessDto updated = repository.setAdminAccess(target.userId(), enabled);
        auditRepository.record(
            actorUserId,
            enabled ? "admin.user.enable" : "admin.user.disable",
            "user",
            target.userId().toString()
        );
        return updated;
    }
}
