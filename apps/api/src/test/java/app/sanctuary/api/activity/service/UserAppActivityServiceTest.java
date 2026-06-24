package app.sanctuary.api.activity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.activity.dto.AnonymousAppActivityRequest;
import app.sanctuary.api.activity.dto.UserAppActivityRequest;
import app.sanctuary.api.activity.repository.UserAppActivityRepository;

@ExtendWith(MockitoExtension.class)
class UserAppActivityServiceTest {

    @Mock
    private UserAppActivityRepository repository;

    @InjectMocks
    private UserAppActivityService service;

    @Test
    void recordRejectsMissingUserId() {
        UserAppActivityRequest request = request();

        assertThrows(IllegalArgumentException.class, () -> service.record(null, request));
    }

    @Test
    void recordPersistsActivityForUser() {
        UUID userId = UUID.randomUUID();
        UserAppActivityRequest request = request();

        service.record(userId, request);

        verify(repository).record(userId, request);
    }

    @Test
    void recordAnonymousPersistsActivityForAnonymousDevice() {
        AnonymousAppActivityRequest request = new AnonymousAppActivityRequest(
            "ios-123",
            "app_open",
            "ios",
            "1.0.12",
            "en",
            "America/New_York",
            null,
            "fcm-token",
            true,
            "ios-instance-1",
            false,
            "app"
        );

        service.recordAnonymous(request);

        verify(repository).recordAnonymous(request);
    }

    private UserAppActivityRequest request() {
        return new UserAppActivityRequest("ios-123", "session_start", "ios", "1.0.12", "en", "America/New_York", "ios-instance-1", false, "app");
    }
}
