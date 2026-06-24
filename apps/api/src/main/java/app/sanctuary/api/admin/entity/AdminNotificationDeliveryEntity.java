package app.sanctuary.api.admin.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import app.sanctuary.api.admin.notification.PushNotificationTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_notification_deliveries")
public class AdminNotificationDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "user_device_id")
    private UUID userDeviceId;

    @Column(name = "anonymous_device_id")
    private String anonymousDeviceId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AdminNotificationDeliveryEntity() {
    }

    public AdminNotificationDeliveryEntity(UUID notificationId, PushNotificationTarget target) {
        this.notificationId = notificationId;
        this.userDeviceId = target.deviceId();
        this.anonymousDeviceId = target.anonymousDeviceId();
        this.userId = target.userId();
        this.platform = target.platform();
        this.status = "targeted";
    }

    public UUID getId() {
        return id;
    }

    public void markSent() {
        status = "sent";
        sentAt = OffsetDateTime.now();
        touch();
    }

    public void markFailed(String reason) {
        status = "failed";
        failureReason = truncate(reason, 1000);
        touch();
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    private void touch() {
        updatedAt = OffsetDateTime.now();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
