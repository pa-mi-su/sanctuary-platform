package app.sanctuary.api.admin.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;

@Repository
public class AdminAuthorizationRepository {

    private final EntityManager entityManager;

    public AdminAuthorizationRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean isAdmin(UUID userId) {
        Long count = entityManager.createQuery(
            """
                SELECT COUNT(adminUser)
                FROM AdminUserEntity adminUser
                WHERE adminUser.userId = :userId
                  AND adminUser.enabled = TRUE
                """,
            Long.class
        ).setParameter("userId", userId).getSingleResult();
        return count > 0;
    }

    public void grantAdmin(UUID userId, String notes) {
        entityManager.createNativeQuery(
            """
                INSERT INTO admin_users (
                    user_id,
                    enabled,
                    notes,
                    created_at,
                    updated_at
                )
                VALUES (
                    :userId,
                    TRUE,
                    :notes,
                    NOW(),
                    NOW()
                )
                ON CONFLICT (user_id)
                DO UPDATE SET
                    enabled = TRUE,
                    notes = COALESCE(admin_users.notes, EXCLUDED.notes),
                    updated_at = NOW()
                """
        )
            .setParameter("userId", userId)
            .setParameter("notes", notes)
            .executeUpdate();
    }
}
