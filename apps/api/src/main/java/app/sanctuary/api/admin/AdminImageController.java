package app.sanctuary.api.admin;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AdminImageController {
    private final AdminAuthService auth;
    private final AdminImages images;
    public AdminImageController(AdminAuthService auth, AdminImages images) { this.auth = auth; this.images = images; }
    @PostMapping(value="/admin-api/images", headers="X-Sanctuary-Admin=1", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object upload(@RequestHeader(value="Authorization", required=false) String token, @RequestParam("image") MultipartFile file) {
        return images.upload(auth.authenticate(token), file);
    }
    @GetMapping("/content/images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        var image = images.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.type()))
            .header("X-Content-Type-Options", "nosniff").header("Content-Disposition", "inline")
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic().immutable()).body(image.data());
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() { return ResponseEntity.status(413).body(Map.of("message", "Choose an image up to 5 MB.")); }
}
