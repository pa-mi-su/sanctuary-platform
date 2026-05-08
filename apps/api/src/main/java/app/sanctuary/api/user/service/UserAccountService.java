package app.sanctuary.api.user.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.repository.UserAccountRepository;
import app.sanctuary.api.user.web.CurrentUser;

@Service
public class UserAccountService {

    private final UserAccountRepository repository;
    private final CognitoAuthService cognitoAuthService;

    public UserAccountService(UserAccountRepository repository, CognitoAuthService cognitoAuthService) {
        this.repository = repository;
        this.cognitoAuthService = cognitoAuthService;
    }

    public UserAccountDto ensureAccount(CurrentUser currentUser) {
        if (currentUser == null || currentUser.cognitoSub() == null || currentUser.cognitoSub().isBlank()) {
            throw new IllegalArgumentException("Authenticated Cognito user is required.");
        }

        return repository.upsert(
            currentUser.cognitoSub(),
            currentUser.email(),
            currentUser.firstName(),
            currentUser.lastName(),
            currentUser.displayName(),
            currentUser.avatarUrl()
        );
    }

    public UserAccountDto updatePreferredLanguage(java.util.UUID userId, String preferredLanguage) {
        return repository.updatePreferredLanguage(userId, preferredLanguage);
    }

    public void deleteAccount(CurrentUser currentUser) {
        UserAccountDto account = ensureAccount(currentUser);
        cognitoAuthService.deleteUser(currentUser.cognitoSub(), currentUser.email());
        repository.deleteById(account.id());
    }
}
