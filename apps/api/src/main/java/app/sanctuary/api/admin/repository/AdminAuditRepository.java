package app.sanctuary.api.admin.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.entity.AdminAuditEventEntity;
import jakarta.persistence.EntityManager;

@Repository
public class AdminAuditRepository {

    private final EntityManager entityManager;

    public AdminAuditRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void record(UUID actorUserId, String action, String targetType, String targetId) {
        entityManager.persist(new AdminAuditEventEntity(actorUserId, action, targetType, targetId));
    }
}
