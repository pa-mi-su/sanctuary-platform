package app.sanctuary.api.user.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class UserAccountRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteByIdClearsOwnedUserDataBeforeDeletingAccount() {
        UserAccountRepository repository = new UserAccountRepository(jdbcTemplate);
        UUID userId = UUID.randomUUID();

        repository.deleteById(userId);

        InOrder inOrder = inOrder(jdbcTemplate);
        inOrder.verify(jdbcTemplate).update(eq("""
                DELETE FROM user_activity_events
                WHERE user_id = ?
                """), eq(userId));
        inOrder.verify(jdbcTemplate).update(eq("""
                DELETE FROM user_preferences
                WHERE user_id = ?
                """), eq(userId));
        inOrder.verify(jdbcTemplate).update(eq("""
                DELETE FROM user_novena_commitments
                WHERE user_id = ?
                """), eq(userId));
        inOrder.verify(jdbcTemplate).update(eq("""
                DELETE FROM user_favorites
                WHERE user_id = ?
                """), eq(userId));
        inOrder.verify(jdbcTemplate).update(eq("""
                DELETE FROM users
                WHERE id = ?
                """), eq(userId));
        inOrder.verifyNoMoreInteractions();
    }
}
