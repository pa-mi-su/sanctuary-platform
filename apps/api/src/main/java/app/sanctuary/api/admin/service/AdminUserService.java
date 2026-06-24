package app.sanctuary.api.admin.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.admin.dto.AdminUsersResponse;
import app.sanctuary.api.admin.repository.AdminUserRepository;

@Service
public class AdminUserService {

    private static final int MAX_LIMIT = 250;

    private final AdminUserRepository repository;

    public AdminUserService(AdminUserRepository repository) {
        this.repository = repository;
    }

    public AdminUsersResponse listUsers(int requestedLimit) {
        int limit = requestedLimit <= 0 ? 50 : Math.min(requestedLimit, MAX_LIMIT);
        return new AdminUsersResponse(repository.metrics(), repository.listRecentDeviceInstalls(limit));
    }
}
