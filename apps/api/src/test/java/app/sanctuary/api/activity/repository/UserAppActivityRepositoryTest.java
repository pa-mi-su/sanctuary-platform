package app.sanctuary.api.activity.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import app.sanctuary.api.activity.dto.AnonymousAppActivityRequest;

@ExtendWith(MockitoExtension.class)
class UserAppActivityRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void anonymousActivityClearsPriorAccountLinkOnLogout() {
        UserAppActivityRepository repository = new UserAppActivityRepository(jdbcTemplate);
        AnonymousAppActivityRequest request = new AnonymousAppActivityRequest(
            "android-device-1",
            "foreground_heartbeat",
            "android",
            "1.0.13-dev",
            "en",
            "America/New_York",
            null,
            "fcm-token",
            true,
            "android-instance-1",
            false,
            "app"
        );
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        repository.recordAnonymous(request);

        verify(jdbcTemplate, atLeastOnce()).update(sqlCaptor.capture(), any(Object[].class));
        assertTrue(
            sqlCaptor.getAllValues().stream().anyMatch(sql -> sql.contains("linked_user_id = EXCLUDED.linked_user_id")),
            "Anonymous heartbeats must be able to clear linked_user_id after logout"
        );
    }
}
