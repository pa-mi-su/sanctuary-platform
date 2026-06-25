package app.sanctuary.api.admin.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.entity.AdminNotificationEntity;
import app.sanctuary.api.admin.notification.PushNotificationTarget;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class AdminNotificationRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TypedQuery<AdminNotificationEntity> notificationQuery;

    @Test
    void historyMaterializesResultsBeforeMappingDtos() throws Exception {
        AdminNotificationRepository repository = new AdminNotificationRepository(entityManager, jdbcTemplate);
        AdminNotificationEntity notification = notification();
        when(entityManager.createQuery(anyString(), eq(AdminNotificationEntity.class))).thenReturn(notificationQuery);
        when(notificationQuery.setMaxResults(10)).thenReturn(notificationQuery);
        when(notificationQuery.getResultList()).thenReturn(List.of(notification));

        List<AdminNotificationDto> history = repository.history(10);

        assertEquals(1, history.size());
        assertEquals("Welcome", history.get(0).title());
    }

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

    private AdminNotificationEntity notification() throws Exception {
        AdminNotificationEntity notification = new AdminNotificationEntity(
            UUID.randomUUID(),
            new app.sanctuary.api.admin.dto.AdminNotificationRequest("Welcome", "Hello")
        );
        setField(notification, "id", UUID.randomUUID());
        setField(notification, "createdAt", OffsetDateTime.now());
        setField(notification, "updatedAt", OffsetDateTime.now());
        return notification;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
