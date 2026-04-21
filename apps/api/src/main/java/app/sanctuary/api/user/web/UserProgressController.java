package app.sanctuary.api.user.web;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.user.dto.UserFavoriteDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentRequest;
import app.sanctuary.api.user.dto.UserPreferencesUpdateRequest;
import app.sanctuary.api.user.dto.UserProfileDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.service.UserProfileService;
import app.sanctuary.api.user.service.UserProgressService;

@RestController
@RequestMapping("/me")
public class UserProgressController {

    private final UserProgressService userProgressService;
    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;

    public UserProgressController(
        UserProgressService userProgressService,
        UserAccountService userAccountService,
        UserProfileService userProfileService
    ) {
        this.userProgressService = userProgressService;
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileDto profile(Authentication authentication) {
        return userProfileService.getProfile(CurrentUser.from(authentication));
    }

    @PutMapping("/preferences")
    public UserProfileDto updatePreferences(
        Authentication authentication,
        @Valid @RequestBody UserPreferencesUpdateRequest request
    ) {
        return userProfileService.updatePreferences(CurrentUser.from(authentication), request);
    }

    @GetMapping("/favorites")
    public List<UserFavoriteDto> favorites(Authentication authentication) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return userProgressService.favorites(account.id());
    }

    @PutMapping("/favorites/{itemType}/{itemId}")
    public ResponseEntity<Void> saveFavorite(
        Authentication authentication,
        @PathVariable String itemType,
        @PathVariable String itemId
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        userProgressService.saveFavorite(account.id(), itemType, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorites/{itemType}/{itemId}")
    public ResponseEntity<Void> deleteFavorite(
        Authentication authentication,
        @PathVariable String itemType,
        @PathVariable String itemId
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        userProgressService.deleteFavorite(account.id(), itemType, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/novena-commitments")
    public List<UserNovenaCommitmentDto> novenaCommitments(Authentication authentication) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return userProgressService.novenaCommitments(account.id());
    }

    @PutMapping("/novena-commitments/{novenaId}")
    public UserNovenaCommitmentDto saveNovenaCommitment(
        Authentication authentication,
        @PathVariable String novenaId,
        @Valid @RequestBody UserNovenaCommitmentRequest request
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return userProgressService.saveNovenaCommitment(account.id(), novenaId, request);
    }

    @DeleteMapping("/novena-commitments/{novenaId}")
    public ResponseEntity<Void> deleteNovenaCommitment(
        Authentication authentication,
        @PathVariable String novenaId
    ) {
        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        userProgressService.deleteNovenaCommitment(account.id(), novenaId);
        return ResponseEntity.noContent().build();
    }
}
