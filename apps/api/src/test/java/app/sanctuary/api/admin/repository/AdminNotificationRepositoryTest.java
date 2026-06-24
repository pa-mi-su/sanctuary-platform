package app.sanctuary.api.admin.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import app.sanctuary.api.admin.notification.PushNotificationTarget;

@ExtendWith(MockitoExtension.class)
class AdminNotificationRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void findValidTargetsUsesCurrentReachableDevicesOnly() {
        AdminNotificationRepository repository = new AdminNotificationRepository(jdbcTemplate);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        when(jdbcTemplate.query(sqlCaptor.capture(), any(RowMapper.class))).thenReturn(List.of());

        List<PushNotificationTarget> targets = repository.findValidTargetsForAllAudience();

        assertEquals(List.of(), targets);
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("last_seen_at >= NOW() - INTERVAL '3 minutes'"));
        assertTrue(sql.contains("notifications_enabled = TRUE"));
        assertTrue(sql.contains("token_status = 'valid'"));
    }
}
