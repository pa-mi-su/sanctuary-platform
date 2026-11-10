package app.sanctuary.api.admin;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@RestController
@RequestMapping(value = "/admin-api", headers = "X-Sanctuary-Admin=1")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminContentService content;
    private final AdminUsersService users;
    private final AdminSessionCookies cookies;
    public AdminController(AdminAuthService auth, AdminContentService content, AdminUsersService users, AdminSessionCookies cookies) { this.auth = auth; this.content = content; this.users = users; this.cookies = cookies; }
    public record Login(@NotBlank @Size(max=254) String email, @NotBlank @Size(max=256) String password) {}
    public record Verify(@NotBlank @Size(max=100) String challenge, @Pattern(regexp="[0-9]{6}") @NotNull String code) {}
    @PostMapping("/login") public Object login(@Valid @RequestBody Login body, HttpServletRequest request) {
        return auth.login(body.email(), body.password(), request.getRemoteAddr());
    }
    public record Password(@NotBlank @Size(max=100) String challenge, @NotBlank @Size(min=14,max=256) String password) {}
    @PostMapping("/password") public Object password(@Valid @RequestBody Password body) { return auth.password(body.challenge(), body.password()); }
    @PostMapping("/verify") public Object verify(@Valid @RequestBody Verify body, @RequestHeader(value="X-Sanctuary-Admin-Client", defaultValue="") String client, jakarta.servlet.http.HttpServletResponse response) {
        var result = auth.verify(body.challenge(), body.code());
        // Keep the existing bearer response for already-loaded clients during a rolling release.
        if (!"cookie-v2".equals(client)) return result;
        response.addHeader("Set-Cookie", cookies.issue(result.get("token")));
        return auth.sessionInfo("Bearer " + result.get("token"));
    }
    @PostMapping("/logout") public Object logout(@RequestHeader(value="Authorization", required=false) String token, jakarta.servlet.http.HttpServletResponse response) { auth.logout(token); response.addHeader("Set-Cookie", cookies.clear()); return Map.of("message", "Signed out"); }
    @GetMapping("/session") public Object session(@RequestHeader(value="Authorization", required=false) String token) { return auth.sessionInfo(token); }
    @GetMapping("/content/{entity}") public Object list(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity, @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="0") int page) {
        auth.authenticate(token); bounds(q, page); return content.list(entity, q, page);
    }
    @GetMapping("/schema/{entity}") public Object schema(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity) { auth.authenticate(token); return content.fields(entity); }
    @GetMapping("/relations/{entity}") public Object relations(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity) { auth.authenticate(token); content.table(entity); return content.relationOptions(entity); }
    @GetMapping("/content/{entity}/{id}") public Object detail(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity, @PathVariable String id) { auth.authenticate(token); return content.detail(entity, id); }
    @PostMapping("/content/{entity}") public Object create(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity, @RequestBody AdminContentService.Save body) { return content.save(auth.authenticate(token), entity, "", body, true); }
    @PutMapping("/content/{entity}/{id}") public Object save(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String entity, @PathVariable String id, @RequestBody AdminContentService.Save body) { return content.save(auth.authenticate(token), entity, id, body, false); }
    public record Term(@NotNull Map<String, String> labels, @NotBlank @Size(min=3,max=1000) String reason) {}
    @PostMapping("/terms/{kind}") public Object term(@RequestHeader(value="Authorization", required=false) String token, @PathVariable String kind, @Valid @RequestBody Term body) { return content.createTerm(auth.authenticate(token), kind, body.labels(), body.reason()); }
    @GetMapping("/audit") public Object audit(@RequestHeader(value="Authorization", required=false) String token, @RequestParam(defaultValue="0") int page) { auth.authenticate(token); bounds("", page); return content.audit(page); }
    @GetMapping("/audit/{id}") public Object auditDetail(@RequestHeader(value="Authorization", required=false) String token, @PathVariable long id) { auth.authenticate(token); return content.auditDetail(id); }
    @GetMapping("/users/summary") public Object summary(@RequestHeader(value="Authorization", required=false) String token) { auth.authenticate(token); return users.summary(); }
    @GetMapping("/users") public Object users(@RequestHeader(value="Authorization", required=false) String token, @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="false") boolean active) { auth.authenticate(token); bounds(q, page); return users.list(q, page, active); }
    @GetMapping("/users/{id}") public Object user(@RequestHeader(value="Authorization", required=false) String token, @PathVariable UUID id, @RequestParam(defaultValue="0") int page) { auth.authenticate(token); bounds("", page); return users.detail(id, page); }
    @GetMapping("/registrations") public Object registrations(@RequestHeader(value="Authorization", required=false) String token, @RequestParam(defaultValue="") String cursor) { auth.authenticate(token); if (cursor.length() > 8192) throw AdminContentService.bad("Invalid cursor."); return users.registrations(cursor); }
    private void bounds(String query, int page) { if (query.length() > 200 || page < 0 || page > 10000) throw AdminContentService.bad("Invalid search or page."); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> invalidContent() { return ResponseEntity.badRequest().body(Map.of("message", "Could not save: check unique IDs, required fields, dates, and referenced content.")); }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class) ResponseEntity<?> invalidRequest() { return ResponseEntity.badRequest().body(Map.of("message", "Check the required fields and try again.")); }
    @ExceptionHandler(CognitoIdentityProviderException.class) ResponseEntity<?> providerFailure() { return ResponseEntity.status(502).body(Map.of("message", "The account directory is unavailable. Check the API's Cognito permissions.")); }
}
