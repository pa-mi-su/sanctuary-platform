package app.sanctuary.api.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.sanctuary.api.auth.service.AuthFlowException;
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

        rejectDeletedIdentity(currentUser);

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

    @Transactional
    public void deleteAccount(CurrentUser currentUser) {
        if (currentUser == null || currentUser.cognitoSub() == null || currentUser.cognitoSub().isBlank()) {
            throw new IllegalArgumentException("Authenticated Cognito user is required.");
        }

        UserAccountDto account = ensureAccount(currentUser);
        cognitoAuthService.deleteUser(currentUser.cognitoSub(), currentUser.email());
        repository.markDeleted(currentUser.cognitoSub(), emailHash(currentUser.email()));
        repository.deleteById(account.id());
    }

    private void rejectDeletedIdentity(CurrentUser currentUser) {
        if (repository.isDeletedIdentity(currentUser.cognitoSub(), null)) {
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "This Sanctuary account has been deleted. Please sign in with another account.");
        }
    }

    private String emailHash(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for deleted account tracking.", exception);
        }
    }
}
