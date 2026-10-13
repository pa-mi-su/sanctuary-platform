package app.sanctuary.api.news;

import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NewsController {
    private final NewsRepository repo;
    public NewsController(NewsRepository repo) { this.repo=repo; }
    @GetMapping("/content/news") public List<NewsArticle> published() { return repo.list(false); }
    @GetMapping("/content/news/{id}") public NewsArticle article(@PathVariable UUID id) { return repo.find(id,false); }
    @GetMapping("/admin/news") public List<NewsArticle> adminList(@RequestParam(defaultValue="false") boolean trash) { return repo.list(true,trash); }
    @GetMapping("/admin/news/{id}") public NewsArticle adminArticle(@PathVariable UUID id) { return repo.find(id,true); }
    @PostMapping("/admin/news") public NewsArticle create(@Valid @RequestBody NewsArticle article, JwtAuthenticationToken user) { return repo.save(UUID.randomUUID(),article,user.getToken().getSubject(),true); }
    @PutMapping("/admin/news/{id}") public NewsArticle update(@PathVariable UUID id,@Valid @RequestBody NewsArticle article,JwtAuthenticationToken user) { return repo.save(id,article,user.getToken().getSubject(),false); }
    @DeleteMapping("/admin/news/{id}") public NewsArticle trash(@PathVariable UUID id,@RequestParam int version,JwtAuthenticationToken user){return repo.trash(id,version,user.getToken().getSubject(),false);}
    @PostMapping("/admin/news/{id}/restore") public NewsArticle restore(@PathVariable UUID id,@RequestParam int version,JwtAuthenticationToken user){return repo.trash(id,version,user.getToken().getSubject(),true);}
    public record CorrectionRequest(@NotBlank @Size(max=2000) String note) {}
    @PostMapping("/admin/news/{id}/corrections") public NewsArticle correct(@PathVariable UUID id,@Valid @RequestBody CorrectionRequest request,JwtAuthenticationToken user) { repo.correct(id,request.note(),user.getToken().getSubject());return repo.find(id,true); }
    @PostMapping("/admin/news/images") public Map<String,UUID> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize()>5242880) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Images must be at most 5 MB");
        // Decode and re-encode raster images. No SVG, active content, filenames, or embedded metadata are stored.
        byte[] data=file.getBytes();
        try (var input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers=ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a JPEG or PNG image");
            var reader=readers.next();
            try {
                reader.setInput(input);
                String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg","png").contains(format) || reader.getWidth(0)>6000 || reader.getHeight(0)>6000 || (long)reader.getWidth(0)*reader.getHeight(0)>20000000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use JPEG/PNG up to 6000 pixels and 20 megapixels");
                var output=new ByteArrayOutputStream();
                ImageIO.write(reader.read(0),format,output);
                if (output.size()>5242880) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Decoded image exceeds 5 MB");
                return Map.of("id",repo.image("jpeg".equals(format)?"image/jpeg":"image/png",output.toByteArray()));
            } finally { reader.dispose(); }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image file",exception);
        }
    }
    @GetMapping("/content/news/images/{id}") public ResponseEntity<byte[]> publicImage(@PathVariable UUID id) { return image(id,false); }
    @GetMapping("/admin/news/images/{id}") public ResponseEntity<byte[]> adminImage(@PathVariable UUID id) { return image(id,true); }
    private ResponseEntity<byte[]> image(UUID id,boolean admin) {
        var image=repo.image(id,admin);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").contentType(MediaType.parseMediaType(image.type())).body(image.bytes());
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("message",error.getReason()==null?"Could not complete the article action.":error.getReason()));}

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> invalidArticle(org.springframework.web.bind.MethodArgumentNotValidException error){
        // Report field names, never rejected draft text or uploaded content in logs/responses.
        String fields=error.getBindingResult().getFieldErrors().stream().map(org.springframework.validation.FieldError::getField).distinct().sorted().collect(java.util.stream.Collectors.joining(", "));
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message","Check these article fields: "+fields));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> malformedJson(){
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message","Invalid request. Check the form and try again."));
    }

}
