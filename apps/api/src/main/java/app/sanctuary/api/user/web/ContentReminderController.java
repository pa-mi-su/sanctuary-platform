package app.sanctuary.api.user.web;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import app.sanctuary.api.user.dto.ContentReminder;
import app.sanctuary.api.user.service.ContentReminderService;
import app.sanctuary.api.user.service.UserAccountService;

@RestController
@RequestMapping("/me/reminders")
public class ContentReminderController {
    private final ContentReminderService reminders;
    private final UserAccountService accounts;
    public ContentReminderController(ContentReminderService reminders, UserAccountService accounts) {
        this.reminders = reminders; this.accounts = accounts;
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> invalid(IllegalArgumentException failure) {
        return ResponseEntity.badRequest().body(java.util.Map.of("message", failure.getMessage()));
    }
    @GetMapping public List<ContentReminder> list(Authentication auth) {
        return reminders.list(accounts.ensureAccount(CurrentUser.from(auth)).id());
    }
    @PutMapping public ResponseEntity<Void> save(Authentication auth, @Valid @RequestBody ContentReminder reminder) {
        reminders.save(accounts.ensureAccount(CurrentUser.from(auth)).id(), reminder);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{kind}/{slug}") public ResponseEntity<Void> delete(Authentication auth, @PathVariable String kind, @PathVariable String slug) {
        reminders.delete(accounts.ensureAccount(CurrentUser.from(auth)).id(), kind, slug);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping public ResponseEntity<Void> clear(Authentication auth) {
        reminders.clear(accounts.ensureAccount(CurrentUser.from(auth)).id());
        return ResponseEntity.noContent().build();
    }
}
