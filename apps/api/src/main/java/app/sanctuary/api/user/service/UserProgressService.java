package app.sanctuary.api.user.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import app.sanctuary.api.user.dto.UserFavoriteDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentRequest;
import app.sanctuary.api.user.repository.UserProgressRepository;

@Service
public class UserProgressService {

    private static final Set<String> SUPPORTED_FAVORITE_TYPES = Set.of("saint", "novena", "prayer");
    private static final Set<String> SUPPORTED_COMMITMENT_STATUSES = Set.of("active", "paused", "completed");

    private final UserProgressRepository repository;

    public UserProgressService(UserProgressRepository repository) {
        this.repository = repository;
    }

    public List<UserFavoriteDto> favorites(String userId) {
        return repository.findFavorites(userId);
    }

    public void saveFavorite(String userId, String itemType, String itemId) {
        validateFavorite(itemType, itemId);
        repository.saveFavorite(userId, itemType, itemId);
    }

    public void deleteFavorite(String userId, String itemType, String itemId) {
        validateFavorite(itemType, itemId);
        repository.deleteFavorite(userId, itemType, itemId);
    }

    public List<UserNovenaCommitmentDto> novenaCommitments(String userId) {
        return repository.findNovenaCommitments(userId);
    }

    public UserNovenaCommitmentDto saveNovenaCommitment(String userId, String novenaId, UserNovenaCommitmentRequest request) {
        if (novenaId == null || novenaId.isBlank()) {
            throw new IllegalArgumentException("Novena id is required.");
        }
        if (!SUPPORTED_COMMITMENT_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("Unsupported commitment status: " + request.status());
        }
        return repository.saveNovenaCommitment(userId, novenaId, request);
    }

    public void deleteNovenaCommitment(String userId, String novenaId) {
        if (novenaId == null || novenaId.isBlank()) {
            throw new IllegalArgumentException("Novena id is required.");
        }
        repository.deleteNovenaCommitment(userId, novenaId);
    }

    private void validateFavorite(String itemType, String itemId) {
        if (!SUPPORTED_FAVORITE_TYPES.contains(itemType)) {
            throw new IllegalArgumentException("Unsupported favorite type: " + itemType);
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Favorite item id is required.");
        }
    }
}
