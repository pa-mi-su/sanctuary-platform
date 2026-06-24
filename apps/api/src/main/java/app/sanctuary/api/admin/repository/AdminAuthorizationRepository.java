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
}
