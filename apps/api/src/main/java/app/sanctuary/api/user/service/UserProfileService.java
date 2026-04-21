package app.sanctuary.api.user.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.user.dto.UserPreferenceDto;
import app.sanctuary.api.user.dto.UserPreferencesUpdateRequest;
import app.sanctuary.api.user.dto.UserProfileCountsDto;
import app.sanctuary.api.user.dto.UserProfileDto;
import app.sanctuary.api.user.repository.UserPreferencesRepository;
import app.sanctuary.api.user.repository.UserProgressRepository;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class UserProfileService {

    private final UserAccountService userAccountService;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserProgressRepository userProgressRepository;

    public UserProfileService(
        UserAccountService userAccountService,
        UserPreferencesRepository userPreferencesRepository,
        UserProgressRepository userProgressRepository
    ) {
        this.userAccountService = userAccountService;
        this.userPreferencesRepository = userPreferencesRepository;
        this.userProgressRepository = userProgressRepository;
    }

    public UserProfileDto getProfile(CurrentUser currentUser) {
        var account = userAccountService.ensureAccount(currentUser);
        UserPreferenceDto preferences = userPreferencesRepository.ensureForUser(account.id());
        UserProfileCountsDto counts = userProgressRepository.profileCounts(account.id());
        return UserProfileDto.from(account, preferences, counts);
    }

    public UserProfileDto updatePreferences(CurrentUser currentUser, UserPreferencesUpdateRequest request) {
        var account = userAccountService.ensureAccount(currentUser);
        UserPreferenceDto preferences = userPreferencesRepository.update(account.id(), request);
        UserProfileCountsDto counts = userProgressRepository.profileCounts(account.id());
        return UserProfileDto.from(account, preferences, counts);
    }
}
