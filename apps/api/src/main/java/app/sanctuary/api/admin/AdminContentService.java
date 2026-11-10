package app.sanctuary.api.admin;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.sanctuary.api.auth.service.AuthFlowException;

@Service
public class AdminContentService {
    // SQL identifiers come exclusively from these server-owned names and database metadata.
    static final Map<String, String> TABLES = Map.ofEntries(
        Map.entry("prayers", "prayers"), Map.entry("saints", "saints"), Map.entry("novenas", "novenas"),
        Map.entry("novena-days", "novena_days"), Map.entry("intentions", "content_intentions"),
        Map.entry("patronages", "content_patronages"));
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final AdminRelations relations;
    public record Field(String name, String type, boolean required, boolean generated) {}
    public record Save(Map<String, Object> values, String version, String reason, Map<String, List<String>> links, List<Map<String, Object>> days) {
        public Save(Map<String, Object> values, String version, String reason) { this(values, version, reason, null, null); }
        public Save(Map<String, Object> values, String version, String reason, Map<String, List<String>> links) { this(values, version, reason, links, null); }
    }
    public AdminContentService(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; this.relations = new AdminRelations(db); }
    String table(String entity) {
        String table = TABLES.get(entity);
        if (table == null) throw new AuthFlowException(HttpStatus.NOT_FOUND, "Unknown content collection.");
        return table;
    }
    public Map<String, Object> relationOptions(String entity) { table(entity); return relations.options(entity); }
    public List<Field> fields(String entity) {
        return db.query("SELECT column_name, data_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name NOT IN ('created_at', 'updated_at') ORDER BY ordinal_position",
            (rs, row) -> new Field(rs.getString(1), rs.getString(2), "NO".equals(rs.getString(3)), rs.getString(4) != null), table(entity));
    }
    public Map<String, Object> list(String entity, String query, int page) {
        String table = table(entity);
        String label = switch (entity) { case "saints" -> "name_en"; case "intentions", "patronages" -> "label_en"; default -> "title_en"; };
        String match = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        var rows = db.queryForList("SELECT id::text AS id, " + label + " AS title" + (entity.equals("novena-days") ? ", novena_id, day_number" : "")
            + " FROM " + table + " WHERE " + label + " ILIKE ? OR id::text ILIKE ? ORDER BY " + label + ", id LIMIT 21 OFFSET ?", match, match, page * 20);
        return Map.of("items", rows.stream().limit(20).toList(), "hasMore", rows.size() > 20);
    }
    public Map<String, Object> detail(String entity, String id) {
        var rows = db.queryForList("SELECT * FROM " + table(entity) + " WHERE id::text = ?", id);
        if (rows.isEmpty()) throw new AuthFlowException(HttpStatus.NOT_FOUND, "Content not found.");
        Map<String, Object> row = new LinkedHashMap<>(rows.getFirst());
        var links = relations.read(entity, id);
        String version = AdminCrypto.hash(serialize(Map.of("values", row, "links", links)));
        row.remove("created_at"); row.remove("updated_at");
        return Map.of("values", row, "version", version, "links", links);
    }
    @Transactional
    public Map<String, Object> save(String actor, String entity, String id, Save request, boolean create) {
        String table = table(entity);
        if (request.values() == null || request.reason() == null || request.reason().strip().length() < 3 || request.reason().length() > 1000)
            throw bad("Enter a change reason between 3 and 1,000 characters.");
        if (create && entity.equals("novenas") && (request.days() == null || request.days().size() != 9 || request.days().stream().anyMatch(Objects::isNull))) throw bad("A new novena requires all nine days before publishing.");
        if (request.days() != null && !(create && entity.equals("novenas"))) throw bad("Day bundles are only accepted when creating a novena.");
        List<Field> schema = fields(entity);
        Map<String, Field> allowed = new LinkedHashMap<>(); schema.forEach(field -> allowed.put(field.name(), field));
        if (!allowed.keySet().containsAll(request.values().keySet())) throw bad("Unknown content field.");
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Field field : schema) {
            Object value = request.values().get(field.name());
            if (create && field.generated() && value == null) continue;
            if (!create && field.name().equals("id")) continue;
            if (value == null && field.required()) throw bad("Required: " + field.name());
            if (value != null && field.type().equals("text") && !(value instanceof String)) throw bad("Enter text: " + field.name());
            if (value instanceof String text) {
                if (Set.of("id", "slug", "name_en", "title_en", "label_en", "novena_id", "category").contains(field.name()) && !(entity.equals("novena-days") && field.name().equals("title_en")) && text.isBlank()) throw bad("Required: " + field.name());
                if (text.length() > 100000) throw bad("Field is too long: " + field.name());
                if ((field.name().equals("slug") || field.name().equals("id") || field.name().equals("novena_id")) && !text.matches("[a-zA-Z0-9_-]{1,200}")) throw bad("Use letters, numbers, hyphens or underscores for identifiers.");
                if (field.name().equals("image_url") && !text.isBlank() && !text.matches("https://[^\\s]+")) throw bad("Image URLs must use HTTPS.");
            }
            if (value != null && (field.type().equals("integer") || field.type().equals("bigint"))) {
                try { value = Long.parseLong(value.toString()); } catch (NumberFormatException e) { throw bad("Enter a whole number: " + field.name()); }
            }
            values.put(field.name(), value);
        }
        if (entity.equals("novenas") && !Objects.equals(values.get("duration_days"), 9L)) throw bad("Sanctuary novenas must contain nine days.");
        if (entity.equals("novena-days")) {
            long day = ((Number) values.get("day_number")).longValue();
            if (day < 1 || day > 9) throw bad("Novena days must be between 1 and 9.");
        }
        if (entity.equals("saints")) {
            try { java.time.LocalDate.of(2000, ((Number) values.get("feast_month")).intValue(), ((Number) values.get("feast_day")).intValue()); }
            catch (java.time.DateTimeException exception) { throw bad("Enter a valid feast month and day."); }
        }
        Map<String, Object> before = null;
        String savedId = id;
        if (create) {
            String columns = String.join(",", values.keySet());
            savedId = db.queryForObject("INSERT INTO " + table + " (" + columns + ") VALUES (" + String.join(",", Collections.nCopies(values.size(), "?")) + ") RETURNING id::text", String.class, values.values().toArray());
        } else {
            var locked = db.queryForList("SELECT * FROM " + table + " WHERE id::text = ? FOR UPDATE", id);
            if (locked.isEmpty()) throw new AuthFlowException(HttpStatus.NOT_FOUND, "Content not found.");
            before = locked.getFirst();
            if (!AdminCrypto.hash(serialize(Map.of("values", before, "links", relations.read(entity, id)))).equals(request.version())) throw new AuthFlowException(HttpStatus.CONFLICT, "This content changed. Reload before editing again.");
            if (values.containsKey("slug") && !Objects.equals(before.get("slug"), values.get("slug"))) throw bad("Existing share-link slugs cannot be changed.");
            if (entity.equals("novena-days") && (!Objects.equals(before.get("novena_id"), values.get("novena_id")) || !Objects.equals(((Number) before.get("day_number")).longValue(), values.get("day_number")))) throw bad("An existing novena day cannot be moved or renumbered.");
            String assignments = String.join(",", values.keySet().stream().map(key -> key + " = ?").toList());
            List<Object> parameters = new ArrayList<>(values.values()); parameters.add(id);
            db.update("UPDATE " + table + " SET " + assignments + ", updated_at = now() WHERE id::text = ?", parameters.toArray());
        }
        if (create && entity.equals("novenas")) {
            for (int day = 1; day <= 9; day++) {
                Map<String, Object> dayValues = new LinkedHashMap<>(request.days().get(day - 1));
                if (!(dayValues.get("prayer_en") instanceof String prayer) || prayer.isBlank()) throw bad("An English prayer is required for every novena day.");
                dayValues.put("novena_id", savedId); dayValues.put("day_number", day);
                save(actor, "novena-days", "", new Save(dayValues, "", request.reason()), true);
            }
        }
        var beforeLinks = create ? Map.of() : relations.read(entity, savedId);
        relations.save(entity, savedId, request.links());
        Map<String, Object> after = detail(entity, savedId);
        db.update("INSERT INTO admin_content_audit(actor_sub, entity, record_id, reason, before_value, after_value) VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb)",
            actor, entity, savedId, request.reason().strip(), before == null ? null : serialize(Map.of("values", before, "links", beforeLinks)), serialize(Map.of("values", after.get("values"), "links", after.get("links"))));
        return after;
    }
    public Map<String, Object> audit(int page) {
        var rows = db.queryForList("SELECT id, entity, record_id, reason, actor_sub, created_at FROM admin_content_audit ORDER BY id DESC LIMIT 21 OFFSET ?", page * 20);
        return Map.of("items", rows.stream().limit(20).toList(), "hasMore", rows.size() > 20);
    }
    public Map<String, Object> auditDetail(long id) {
        var rows = db.queryForList("SELECT id, entity, record_id, reason, actor_sub, created_at, before_value::text, after_value::text FROM admin_content_audit WHERE id = ?", id);
        if (rows.isEmpty()) throw new AuthFlowException(HttpStatus.NOT_FOUND, "Change not found.");
        return rows.getFirst();
    }
    private String serialize(Object value) {
        try { return json.writer().with(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Unable to serialize content"); }
    }
    static AuthFlowException bad(String message) { return new AuthFlowException(HttpStatus.BAD_REQUEST, message); }
}
