package app.sanctuary.api.admin;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminImages {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private final JdbcTemplate db;
    public AdminImages(JdbcTemplate db) { this.db = db; }
    public record Image(byte[] data, String type) {}
    @Transactional
    public Map<String, Object> upload(String actor, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw AdminContentService.bad("Choose a JPEG or PNG image up to 5 MB.");
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw AdminContentService.bad("This file is not a supported image. Choose JPEG or PNG.");
            var reader = readers.next();
            BufferedImage image; int width; int height; String format;
            try {
                reader.setInput(stream, true, true); format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg", "jpg", "png").contains(format)) throw AdminContentService.bad("Choose JPEG or PNG; SVG and animated formats are not supported.");
                width = reader.getWidth(0); height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 16000000)
                    throw AdminContentService.bad("Image dimensions must fit within 4096 × 4096 pixels (16 megapixels).");
                image = reader.read(0);
            } finally { reader.dispose(); }
            // Re-encode pixels rather than serving uploaded bytes/metadata verbatim.
            var output = new ByteArrayOutputStream();
            String outputFormat = format.equals("png") ? "png" : "jpeg";
            if (!ImageIO.write(image, outputFormat, output)) throw AdminContentService.bad("Could not encode this image.");
            byte[] bytes = output.toByteArray();
            if (bytes.length > MAX_BYTES) throw AdminContentService.bad("The processed image exceeds 5 MB. Resize it and try again.");
            UUID id = UUID.randomUUID(); String type = "image/" + outputFormat;
            db.update("INSERT INTO admin_content_images(id, content_type, data, width, height, created_by) VALUES (?, ?, ?, ?, ?, ?)", id, type, bytes, width, height, actor);
            db.update("INSERT INTO admin_content_audit(actor_sub, entity, record_id, reason, after_value) VALUES (?, 'images', ?, 'Upload content image', jsonb_build_object('contentType', ?::text, 'width', ?::int, 'height', ?::int, 'bytes', ?::int))", actor, id.toString(), type, width, height, bytes.length);
            return Map.of("id", id, "path", "/content/images/" + id, "width", width, "height", height);
        } catch (IOException | IllegalArgumentException exception) { throw AdminContentService.bad("The image could not be read. Choose a valid JPEG or PNG."); }
    }
    public Image get(UUID id) {
        return db.query("SELECT data, content_type FROM admin_content_images WHERE id = ?", (rs, row) -> new Image(rs.getBytes(1), rs.getString(2)), id).stream().findFirst()
            .orElseThrow(() -> new app.sanctuary.api.auth.service.AuthFlowException(org.springframework.http.HttpStatus.NOT_FOUND, "Image not found."));
    }
}
