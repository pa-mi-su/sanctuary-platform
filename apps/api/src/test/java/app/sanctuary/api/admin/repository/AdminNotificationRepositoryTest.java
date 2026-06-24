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
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class AdminNotificationRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void findValidTargetsUsesReachableDevicesDedupedByInstall() {
        AdminNotificationRepository repository = new AdminNotificationRepository(entityManager, jdbcTemplate);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        when(jdbcTemplate.query(sqlCaptor.capture(), any(RowMapper.class))).thenReturn(List.of());

        List<PushNotificationTarget> targets = repository.findValidTargetsForAllAudience();

        assertEquals(List.of(), targets);
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), '')"));
        assertTrue(sql.contains("PARTITION BY install_key"));
        assertTrue(sql.contains("notifications_enabled = TRUE"));
        assertTrue(sql.contains("token_status = 'valid'"));
        assertTrue(sql.contains("NULLIF(TRIM(fcm_token), '') IS NOT NULL"));
    }
}
