package app.sanctuary.api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.dto.UserPreferenceDto;
import app.sanctuary.api.user.dto.UserPreferencesUpdateRequest;
import app.sanctuary.api.user.dto.UserProfileCountsDto;
import app.sanctuary.api.user.repository.UserPreferencesRepository;
import app.sanctuary.api.user.repository.UserProgressRepository;
import app.sanctuary.api.user.web.CurrentUser;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private UserProfileService service;

    @Test
    void getProfileBuildsAggregateView() {
        CurrentUser currentUser = new CurrentUser("sub-1", "saint@example.com", "Saint User", "https://example.com/avatar.png");
        UUID userId = UUID.randomUUID();
        UserAccountDto account = new UserAccountDto(userId, "sub-1", "saint@example.com", "Saint User", "en", null, OffsetDateTime.now(), OffsetDateTime.now());
        UserPreferenceDto preferences = new UserPreferenceDto(userId, "America/New_York", true, false, true, true, OffsetDateTime.now(), OffsetDateTime.now());
        UserProfileCountsDto counts = new UserProfileCountsDto(2, 3, 1, 4, 5);

        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(userPreferencesRepository.ensureForUser(userId)).thenReturn(preferences);
        when(userProgressRepository.profileCounts(userId)).thenReturn(counts);

        var result = service.getProfile(currentUser);

        assertEquals(userId.toString(), result.userId());
        assertEquals("America/New_York", result.timeZoneId());
        assertEquals(3, result.favoriteNovenaCount());
        assertEquals(4, result.activeNovenaCount());
    }

    @Test
    void updatePreferencesReturnsRefreshedProfile() {
        CurrentUser currentUser = new CurrentUser("sub-1", "saint@example.com", "Saint User", "https://example.com/avatar.png");
        UUID userId = UUID.randomUUID();
        UserAccountDto account = new UserAccountDto(userId, "sub-1", "saint@example.com", "Saint User", "en", null, OffsetDateTime.now(), OffsetDateTime.now());
        UserPreferencesUpdateRequest request = new UserPreferencesUpdateRequest("Europe/Warsaw", true, true, false, true);
        UserPreferenceDto preferences = new UserPreferenceDto(userId, "Europe/Warsaw", true, true, false, true, OffsetDateTime.now(), OffsetDateTime.now());
        UserProfileCountsDto counts = new UserProfileCountsDto(1, 1, 0, 2, 0);

        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(userPreferencesRepository.update(userId, request)).thenReturn(preferences);
        when(userProgressRepository.profileCounts(userId)).thenReturn(counts);

        var result = service.updatePreferences(currentUser, request);

        assertEquals("Europe/Warsaw", result.timeZoneId());
        assertEquals(2, result.activeNovenaCount());
        verify(userPreferencesRepository).update(userId, request);
    }
}
