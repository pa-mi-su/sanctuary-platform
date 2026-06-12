package app.sanctuary.api.user.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import app.sanctuary.api.user.dto.UserAccountDto;

@ExtendWith(MockitoExtension.class)
class UserAccountRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void upsertReusesExistingAccountByEmailBeforeInsertingByCognitoSub() throws SQLException {
        UserAccountRepository repository = new UserAccountRepository(jdbcTemplate);
        UUID existingUserId = UUID.randomUUID();
        ResultSet resultSet = accountResultSet(
            existingUserId,
            "new-cognito-sub",
            "saint@example.com",
            "Saint User"
        );
        when(jdbcTemplate.query(
            eq("""
                UPDATE users
                SET
                    cognito_sub = ?,
                    email = ?,
                    first_name = COALESCE(?, users.first_name),
                    last_name = COALESCE(?, users.last_name),
                    display_name = COALESCE(?, users.display_name),
                    avatar_url = COALESCE(?, users.avatar_url),
                    last_sign_in_at = NOW(),
                    updated_at = NOW()
                WHERE LOWER(email) = LOWER(?)
                RETURNING
                    id,
                    cognito_sub,
                    email,
                    first_name,
                    last_name,
                    display_name,
                    preferred_language,
                    avatar_url,
                    created_at,
                    updated_at
                """),
            any(RowMapper.class),
            eq("new-cognito-sub"),
            eq("saint@example.com"),
            eq("Saint"),
            eq("User"),
            eq("Saint User"),
            eq("https://example.com/avatar.png"),
            eq("saint@example.com")
        ))
            .thenAnswer(invocation -> List.of(((RowMapper<UserAccountDto>) invocation.getArgument(1)).mapRow(resultSet, 0)));

        UserAccountDto account = repository.upsert(
            "new-cognito-sub",
            "  SAINT@example.com  ",
            "Saint",
            "User",
            "Saint User",
            "https://example.com/avatar.png"
        );

        org.assertj.core.api.Assertions.assertThat(account.id()).isEqualTo(existingUserId);
        org.assertj.core.api.Assertions.assertThat(account.cognitoSub()).isEqualTo("new-cognito-sub");
        org.assertj.core.api.Assertions.assertThat(account.email()).isEqualTo("saint@example.com");
        verify(jdbcTemplate, never()).queryForObject(any(String.class), any(RowMapper.class), any());
    }

    @Test
    void upsertFallsBackToCognitoSubInsertWhenNoEmailAccountExists() {
        UserAccountRepository repository = new UserAccountRepository(jdbcTemplate);
        UserAccountDto inserted = new UserAccountDto(
            UUID.randomUUID(),
            "new-cognito-sub",
            "saint@example.com",
            "Saint",
            "User",
            "Saint User",
            "en",
            "https://example.com/avatar.png",
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
        when(jdbcTemplate.query(
            any(String.class),
            any(RowMapper.class),
            eq("new-cognito-sub"),
            eq("saint@example.com"),
            eq("Saint"),
            eq("User"),
            eq("Saint User"),
            eq("https://example.com/avatar.png"),
            eq("saint@example.com")
        ))
            .thenReturn(List.of());
        when(jdbcTemplate.queryForObject(
            any(String.class),
            any(RowMapper.class),
            eq("new-cognito-sub"),
            eq("saint@example.com"),
            eq("Saint"),
            eq("User"),
            eq("Saint User"),
            eq("https://example.com/avatar.png")
        ))
            .thenReturn(inserted);

        UserAccountDto account = repository.upsert(
            "new-cognito-sub",
            "SAINT@example.com",
            "Saint",
            "User",
            "Saint User",
            "https://example.com/avatar.png"
        );

        org.assertj.core.api.Assertions.assertThat(account).isEqualTo(inserted);
    }

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

    private ResultSet accountResultSet(UUID id, String cognitoSub, String email, String displayName) throws SQLException {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        when(resultSet.getString("cognito_sub")).thenReturn(cognitoSub);
        when(resultSet.getString("email")).thenReturn(email);
        when(resultSet.getString("first_name")).thenReturn("Saint");
        when(resultSet.getString("last_name")).thenReturn("User");
        when(resultSet.getString("display_name")).thenReturn(displayName);
        when(resultSet.getString("preferred_language")).thenReturn("en");
        when(resultSet.getString("avatar_url")).thenReturn("https://example.com/avatar.png");
        when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        return resultSet;
    }
}
