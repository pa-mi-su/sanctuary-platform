package app.sanctuary.api.admin;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminRelations {
    record Relation(String table, String owner, String value, String catalogue) {}
    private final JdbcTemplate db;
    public AdminRelations(JdbcTemplate db) { this.db = db; }
    private Map<String, Relation> definitions(String entity) {
        Map<String, Relation> result = new TreeMap<>();
        String singular = switch (entity) { case "saints" -> "saint"; case "novenas" -> "novena"; case "prayers" -> "prayer"; default -> ""; };
        if (!singular.isBlank()) result.put("tags", new Relation(singular + "_tags", singular + "_id", "tag", ""));
        if (entity.equals("saints") || entity.equals("novenas")) result.put("intentions", new Relation(singular + "_intention_links", singular + "_id", "intention_id", "content_intentions"));
        if (entity.equals("saints")) result.put("patronages", new Relation("saint_patronage_links", "saint_id", "patronage_id", "content_patronages"));
        return result;
    }
    public Map<String, List<String>> read(String entity, String id) {
        Map<String, List<String>> result = new TreeMap<>();
        definitions(entity).forEach((key, spec) -> result.put(key, db.query("SELECT " + spec.value() + "::text FROM " + spec.table() + " WHERE " + spec.owner() + " = ? ORDER BY " + spec.value(), (rs, row) -> rs.getString(1), id)));
        return result;
    }
    public Map<String, Object> options(String entity) {
        Map<String, Object> result = new TreeMap<>();
        definitions(entity).forEach((key, spec) -> result.put(key, spec.catalogue().isBlank() ? List.of() : db.queryForList("SELECT id::text AS id, label_en AS title FROM " + spec.catalogue() + " ORDER BY label_en LIMIT 5000")));
        return result;
    }
    public void save(String entity, String id, Map<String, List<String>> links) {
        if (links == null) return;
        var definitions = definitions(entity);
        if (!definitions.keySet().equals(links.keySet())) throw AdminContentService.bad("Invalid content relationships.");
        definitions.forEach((key, spec) -> {
            List<String> values = links.get(key);
            if (values == null || values.size() > 200 || new HashSet<>(values).size() != values.size()) throw AdminContentService.bad("Invalid or duplicate relationships.");
            for (String value : values) if (value == null || value.isBlank() || value.length() > 200 || (!spec.catalogue().isBlank() && !value.matches("[0-9]+"))) throw AdminContentService.bad("Invalid relationship value.");
            db.update("DELETE FROM " + spec.table() + " WHERE " + spec.owner() + " = ?", id);
            for (String value : values) db.update("INSERT INTO " + spec.table() + " (" + spec.owner() + "," + spec.value() + ") VALUES (?, ?)", id, spec.catalogue().isBlank() ? value : Long.valueOf(value));
        });
        if (entity.equals("novenas")) syncLegacyIntentions(id);
        if (entity.equals("intentions")) {
            db.query("SELECT novena_id FROM novena_intention_links WHERE intention_id::text = ?", (rs, n) -> rs.getString(1), id).forEach(this::syncLegacyIntentions);
        }
    }
    private void syncLegacyIntentions(String id) {
        db.update("DELETE FROM novena_intentions WHERE novena_id = ?", id);
        for (String locale : List.of("en", "es", "pl"))
            db.update("INSERT INTO novena_intentions(novena_id, locale, intention_text, sort_order) SELECT nil.novena_id, ?, ci.label_" + locale + ", (row_number() OVER (ORDER BY nil.sort_order, ci.id) - 1)::integer FROM novena_intention_links nil JOIN content_intentions ci ON ci.id = nil.intention_id WHERE nil.novena_id = ?", locale, id);
    }
}
