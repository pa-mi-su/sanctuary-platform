package app.sanctuary.api.admin.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_notifications")
public class AdminNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "audience_type", nullable = false)
    private String audienceType = "all";

    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "sent_by_user_id")
    private UUID sentByUserId;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AdminNotificationEntity() {
    }

    public AdminNotificationEntity(UUID createdByUserId, AdminNotificationRequest request) {
        this.createdByUserId = createdByUserId;
        this.title = request.title().trim();
        this.message = request.message().trim();
    }

    public UUID getId() {
        return id;
    }

    public boolean markSending(UUID sentByUserId, int targetCount) {
        if (!"draft".equals(status)) {
            return false;
        }
        status = "sending";
        this.sentByUserId = sentByUserId;
        this.targetCount = targetCount;
        sentCount = 0;
        failedCount = 0;
        touch();
        return true;
    }

    public void finishSend(String status, int sentCount, int failedCount) {
        this.status = status;
        this.sentCount = sentCount;
        this.failedCount = failedCount;
        sentAt = OffsetDateTime.now();
        touch();
    }

    public AdminNotificationDto toDto() {
        return new AdminNotificationDto(
            id,
            title,
            message,
            audienceType,
            status,
            targetCount,
            sentCount,
            failedCount,
            sentAt,
            createdAt,
            updatedAt
        );
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
}
