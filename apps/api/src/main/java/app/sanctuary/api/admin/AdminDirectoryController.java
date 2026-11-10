package app.sanctuary.api.admin;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/admin-api/directory", headers="X-Sanctuary-Admin=1")
public class AdminDirectoryController {
    private final AdminAuthService auth;
    private final AdminDirectory directory;
    public AdminDirectoryController(AdminAuthService auth, AdminDirectory directory) { this.auth=auth; this.directory=directory; }
    @GetMapping public Object list(@RequestHeader(value="Authorization", required=false) String token,
        @RequestParam(defaultValue="") String q, @RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="all") String status, @RequestParam(defaultValue="all") String activity,
        @RequestParam(defaultValue="false") boolean refresh) {
        auth.authenticate(token); return directory.list(q, page, status, activity, refresh);
    }
    @ExceptionHandler(software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException.class)
    org.springframework.http.ResponseEntity<?> unavailable() { return org.springframework.http.ResponseEntity.status(502).body(java.util.Map.of("message", "Could not refresh the account directory. Please try again; the previous snapshot has been retained.")); }
}
