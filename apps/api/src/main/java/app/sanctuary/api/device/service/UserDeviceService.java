package app.sanctuary.api.device.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.sanctuary.api.device.dto.UserDeviceDto;
import app.sanctuary.api.device.dto.UserDeviceRegistrationRequest;
import app.sanctuary.api.device.repository.UserDeviceRepository;

@Service
public class UserDeviceService {

    private final UserDeviceRepository repository;

    public UserDeviceService(UserDeviceRepository repository) {
        this.repository = repository;
    }

    public UserDeviceDto register(UUID userId, UserDeviceRegistrationRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required to register a device.");
        }
        return repository.upsert(userId, request);
    }

    public List<UserDeviceDto> list(UUID userId) {
        return repository.findByUserId(userId);
    }
}
