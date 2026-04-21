package app.sanctuary.api.user.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.repository.UserAccountRepository;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public UserAccountDto ensureAccount(CurrentUser currentUser) {
        if (currentUser == null || currentUser.cognitoSub() == null || currentUser.cognitoSub().isBlank()) {
            throw new IllegalArgumentException("Authenticated Cognito user is required.");
        }

        return repository.upsert(
            currentUser.cognitoSub(),
            currentUser.email(),
            currentUser.displayName(),
            currentUser.avatarUrl()
        );
    }

    public UserAccountDto updatePreferredLanguage(java.util.UUID userId, String preferredLanguage) {
        return repository.updatePreferredLanguage(userId, preferredLanguage);
    }
}
