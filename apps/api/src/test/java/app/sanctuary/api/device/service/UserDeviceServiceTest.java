package app.sanctuary.api.device.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.device.dto.UserDeviceDto;
import app.sanctuary.api.device.dto.UserDeviceRegistrationRequest;
import app.sanctuary.api.device.repository.UserDeviceRepository;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceTest {

    @Mock
    private UserDeviceRepository repository;

    @InjectMocks
    private UserDeviceService service;

    @Test
    void registerRejectsMissingUserId() {
        UserDeviceRegistrationRequest request = request();

        assertThrows(IllegalArgumentException.class, () -> service.register(null, request));
    }

    @Test
    void registerUpsertsDeviceForUser() {
        UUID userId = UUID.randomUUID();
        UserDeviceRegistrationRequest request = request();
        UserDeviceDto device = device(userId);
        when(repository.upsert(userId, request)).thenReturn(device);

        UserDeviceDto result = service.register(userId, request);

        assertEquals(device, result);
        verify(repository).upsert(userId, request);
    }

    @Test
    void listReturnsDevicesForUser() {
        UUID userId = UUID.randomUUID();
        List<UserDeviceDto> devices = List.of(device(userId));
        when(repository.findByUserId(userId)).thenReturn(devices);

        List<UserDeviceDto> result = service.list(userId);

        assertEquals(devices, result);
    }

    private UserDeviceRegistrationRequest request() {
        return new UserDeviceRegistrationRequest("token-123", "ios", "1.0.12", "en", true, "ios-instance-1", false, "app");
    }

    private UserDeviceDto device(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserDeviceDto(
            UUID.randomUUID(),
            userId,
            "ios",
            "1.0.12",
            "en",
            true,
            "valid",
            now,
            now,
            now
        );
    }
}
