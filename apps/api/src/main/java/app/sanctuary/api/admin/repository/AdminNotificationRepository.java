package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.entity.AdminNotificationDeliveryEntity;
import app.sanctuary.api.admin.entity.AdminNotificationEntity;
import app.sanctuary.api.admin.notification.PushNotificationTarget;
import jakarta.persistence.EntityManager;

@Repository
public class AdminNotificationRepository {

    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    public AdminNotificationRepository(EntityManager entityManager, JdbcTemplate jdbcTemplate) {
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminNotificationDto createDraft(UUID createdByUserId, AdminNotificationRequest request) {
        AdminNotificationEntity notification = new AdminNotificationEntity(createdByUserId, request);
        entityManager.persist(notification);
        entityManager.flush();
        return notification.toDto();
    }

    public List<AdminNotificationDto> history(int limit) {
        return entityManager.createQuery(
            """
                SELECT notification
                FROM AdminNotificationEntity notification
                ORDER BY notification.createdAt DESC
                """,
            AdminNotificationEntity.class
        ).setMaxResults(limit).getResultStream()
            .map(AdminNotificationEntity::toDto)
            .toList();
    }

    public boolean markSending(UUID notificationId, UUID sentByUserId, int targetCount) {
        AdminNotificationEntity notification = entityManager.find(AdminNotificationEntity.class, notificationId);
        return notification != null && notification.markSending(sentByUserId, targetCount);
    }

    public void finishSend(UUID notificationId, String status, int sentCount, int failedCount) {
        AdminNotificationEntity notification = entityManager.find(AdminNotificationEntity.class, notificationId);
        if (notification != null) {
            notification.finishSend(status, sentCount, failedCount);
        }
    }

    public List<PushNotificationTarget> findValidTargetsForAllAudience() {
        return jdbcTemplate.query(
            """
                WITH targets AS (
                    SELECT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), id::text) AS install_key,
                        id AS user_device_id,
                        NULL::text AS anonymous_device_id,
                        user_id,
                        platform,
                        fcm_token,
                        updated_at,
                        0 AS priority
                    FROM user_devices
                    WHERE notifications_enabled = TRUE
                      AND token_status = 'valid'
                      AND automated_test = FALSE
                      AND check_in_source = 'app'
                      AND NULLIF(TRIM(fcm_token), '') IS NOT NULL

                    UNION ALL

                    SELECT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), anonymous_device_id) AS install_key,
                        NULL::uuid AS user_device_id,
                        anonymous_device_id,
                        linked_user_id AS user_id,
                        platform,
                        fcm_token,
                        updated_at,
                        1 AS priority
                    FROM anonymous_app_devices
                    WHERE notifications_enabled = TRUE
                      AND token_status = 'valid'
                      AND linked_user_id IS NULL
                      AND automated_test = FALSE
                      AND check_in_source = 'app'
                      AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                ),
                ranked AS (
                    SELECT
                        *,
                        ROW_NUMBER() OVER (
                            PARTITION BY install_key
                            ORDER BY priority ASC, updated_at DESC
                        ) AS install_rank
                    FROM targets
                )
                SELECT
                    user_device_id,
                    anonymous_device_id,
                    user_id,
                    platform,
                    fcm_token
                FROM ranked
                WHERE install_rank = 1
                ORDER BY updated_at DESC
                """,
            (rs, rowNum) -> new PushNotificationTarget(
                rs.getObject("user_device_id", UUID.class),
                rs.getString("anonymous_device_id"),
                rs.getObject("user_id", UUID.class),
                rs.getString("platform"),
                rs.getString("fcm_token")
            )
        );
    }

    public UUID createDelivery(UUID notificationId, PushNotificationTarget target) {
        AdminNotificationDeliveryEntity delivery = new AdminNotificationDeliveryEntity(notificationId, target);
        entityManager.persist(delivery);
        entityManager.flush();
        return delivery.getId();
    }

    public void markDeliverySent(UUID deliveryId) {
        AdminNotificationDeliveryEntity delivery = entityManager.find(AdminNotificationDeliveryEntity.class, deliveryId);
        if (delivery != null) {
            delivery.markSent();
        }
    }

    public void markDeliveryFailed(UUID deliveryId, String failureReason) {
        AdminNotificationDeliveryEntity delivery = entityManager.find(AdminNotificationDeliveryEntity.class, deliveryId);
        if (delivery != null) {
            delivery.markFailed(failureReason);
        }
    }

    public void markDeviceInvalid(UUID deviceId) {
        if (deviceId == null) {
            return;
        }
        jdbcTemplate.update(
            """
                UPDATE user_devices
                SET
                    token_status = 'invalid',
                    updated_at = NOW()
                WHERE id = ?
                """,
            deviceId
        );
    }

    public void markAnonymousDeviceInvalid(String anonymousDeviceId) {
        if (anonymousDeviceId == null || anonymousDeviceId.isBlank()) {
            return;
        }
        jdbcTemplate.update(
            """
                UPDATE anonymous_app_devices
                SET
                    token_status = 'invalid',
                    updated_at = NOW()
                WHERE anonymous_device_id = ?
                """,
            anonymousDeviceId
        );
    }

}
