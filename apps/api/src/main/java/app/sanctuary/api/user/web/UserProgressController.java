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
import app.sanctuary.api.user.dto.UserProfileDto;
import app.sanctuary.api.user.service.UserProgressService;

@RestController
@RequestMapping("/me")
public class UserProgressController {

    private final UserProgressService userProgressService;

    public UserProgressController(UserProgressService userProgressService) {
        this.userProgressService = userProgressService;
    }

    @GetMapping
    public UserProfileDto profile(Authentication authentication) {
        CurrentUser user = CurrentUser.from(authentication);
        return new UserProfileDto(user.id(), user.email(), user.displayName());
    }

    @GetMapping("/favorites")
    public List<UserFavoriteDto> favorites(Authentication authentication) {
        return userProgressService.favorites(CurrentUser.from(authentication).id());
    }

    @PutMapping("/favorites/{itemType}/{itemId}")
    public ResponseEntity<Void> saveFavorite(
        Authentication authentication,
        @PathVariable String itemType,
        @PathVariable String itemId
    ) {
        userProgressService.saveFavorite(CurrentUser.from(authentication).id(), itemType, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/favorites/{itemType}/{itemId}")
    public ResponseEntity<Void> deleteFavorite(
        Authentication authentication,
        @PathVariable String itemType,
        @PathVariable String itemId
    ) {
        userProgressService.deleteFavorite(CurrentUser.from(authentication).id(), itemType, itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/novena-commitments")
    public List<UserNovenaCommitmentDto> novenaCommitments(Authentication authentication) {
        return userProgressService.novenaCommitments(CurrentUser.from(authentication).id());
    }

    @PutMapping("/novena-commitments/{novenaId}")
    public UserNovenaCommitmentDto saveNovenaCommitment(
        Authentication authentication,
        @PathVariable String novenaId,
        @Valid @RequestBody UserNovenaCommitmentRequest request
    ) {
        return userProgressService.saveNovenaCommitment(CurrentUser.from(authentication).id(), novenaId, request);
    }

    @DeleteMapping("/novena-commitments/{novenaId}")
    public ResponseEntity<Void> deleteNovenaCommitment(
        Authentication authentication,
        @PathVariable String novenaId
    ) {
        userProgressService.deleteNovenaCommitment(CurrentUser.from(authentication).id(), novenaId);
        return ResponseEntity.noContent().build();
    }
}
