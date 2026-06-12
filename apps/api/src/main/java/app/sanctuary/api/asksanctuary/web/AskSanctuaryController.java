package app.sanctuary.api.asksanctuary.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryRequest;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryStatusResponse;
import app.sanctuary.api.asksanctuary.service.AskSanctuaryService;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@RestController
public class AskSanctuaryController {

    private final AskSanctuaryService service;
    private final UserAccountService userAccountService;

    public AskSanctuaryController(AskSanctuaryService service, UserAccountService userAccountService) {
        this.service = service;
        this.userAccountService = userAccountService;
    }

    @GetMapping({"/ask-sanctuary/status", "/api/ask-sanctuary/status"})
    public ResponseEntity<AskSanctuaryStatusResponse> status(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AskSanctuaryStatusResponse("v1", false));
        }

        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return ResponseEntity.ok(service.status(account.id()));
    }

    @PostMapping({"/ask-sanctuary/disclaimer", "/api/ask-sanctuary/disclaimer"})
    public ResponseEntity<AskSanctuaryStatusResponse> acceptDisclaimer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AskSanctuaryStatusResponse("v1", false));
        }

        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return ResponseEntity.ok(service.acceptDisclaimer(account.id()));
    }

    @PostMapping({"/ask-sanctuary", "/api/ask-sanctuary"})
    public ResponseEntity<AskSanctuaryResponse> ask(
        Authentication authentication,
        HttpServletRequest servletRequest,
        @Valid @RequestBody AskSanctuaryRequest request
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AskSanctuaryResponse.accountRequired());
        }

        var account = userAccountService.ensureAccount(CurrentUser.from(authentication));
        return ResponseEntity.ok(service.answer(account.id(), request.message(), clientIpHash(servletRequest)).response());
    }

    private String clientIpHash(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
            ? request.getRemoteAddr()
            : forwardedFor.split(",")[0].trim();

        if (ip == null || ip.isBlank()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available.", exception);
        }
    }
}
