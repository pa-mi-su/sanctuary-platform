package app.sanctuary.api.activity.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import app.sanctuary.api.activity.dto.AnonymousAppActivityRequest;
import app.sanctuary.api.activity.dto.UserAppActivityRequest;
import app.sanctuary.api.activity.repository.UserAppActivityRepository;

@Service
public class UserAppActivityService {

    private final UserAppActivityRepository repository;

    public UserAppActivityService(UserAppActivityRepository repository) {
        this.repository = repository;
    }

    public void record(UUID userId, UserAppActivityRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required to record app activity.");
        }
        repository.record(userId, request);
    }

    public void recordAnonymous(AnonymousAppActivityRequest request) {
        repository.recordAnonymous(request);
    }
}
