package app.sanctuary.api.user.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.user.dto.UserPreferenceDto;
import app.sanctuary.api.user.dto.UserPreferencesUpdateRequest;
import app.sanctuary.api.user.dto.UserProfileCountsDto;
import app.sanctuary.api.user.dto.UserProfileDto;
import app.sanctuary.api.user.dto.UserStreakSummaryDto;
import app.sanctuary.api.user.repository.UserPreferencesRepository;
import app.sanctuary.api.user.repository.UserProgressRepository;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class UserProfileService {
    private static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of("en", "es", "pl");

    private final UserAccountService userAccountService;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserActivityService userActivityService;

    public UserProfileService(
        UserAccountService userAccountService,
        UserPreferencesRepository userPreferencesRepository,
        UserProgressRepository userProgressRepository,
        UserActivityService userActivityService
    ) {
        this.userAccountService = userAccountService;
        this.userPreferencesRepository = userPreferencesRepository;
        this.userProgressRepository = userProgressRepository;
        this.userActivityService = userActivityService;
    }

    public UserProfileDto getProfile(CurrentUser currentUser) {
        var account = userAccountService.ensureAccount(currentUser);
        UserPreferenceDto preferences = userPreferencesRepository.ensureForUser(account.id());
        UserProfileCountsDto counts = userProgressRepository.profileCounts(account.id());
        UserStreakSummaryDto streakSummary = userActivityService.streakSummary(account.id(), preferences.timeZoneId());
        return UserProfileDto.from(account, preferences, counts, streakSummary);
    }

    public UserProfileDto updatePreferences(CurrentUser currentUser, UserPreferencesUpdateRequest request) {
        validateLanguage(request.preferredLanguage());

        var account = userAccountService.ensureAccount(currentUser);
        var updatedAccount = userAccountService.updatePreferredLanguage(account.id(), request.preferredLanguage());
        UserPreferenceDto preferences = userPreferencesRepository.update(updatedAccount.id(), request);
        UserProfileCountsDto counts = userProgressRepository.profileCounts(updatedAccount.id());
        UserStreakSummaryDto streakSummary = userActivityService.streakSummary(updatedAccount.id(), preferences.timeZoneId());
        return UserProfileDto.from(updatedAccount, preferences, counts, streakSummary);
    }

    private void validateLanguage(String preferredLanguage) {
        if (!SUPPORTED_LANGUAGES.contains(preferredLanguage)) {
            throw new IllegalArgumentException("Unsupported preferred language: " + preferredLanguage);
        }
    }
}
