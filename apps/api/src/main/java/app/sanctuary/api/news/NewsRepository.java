package app.sanctuary.api.news;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Repository
public class NewsRepository {
    private final JdbcTemplate db;
    public NewsRepository(JdbcTemplate db) { this.db = db; }
    private static final String PUBLIC = "deleted_at IS NULL AND status IN ('PUBLISHED','SCHEDULED') AND publish_at <= now()";
    public List<NewsArticle> list(boolean admin) { return list(admin,false); }
    public List<NewsArticle> list(boolean admin,boolean trash) {
        String filter=admin?(trash?"deleted_at IS NOT NULL":"deleted_at IS NULL"):PUBLIC;
        return db.query("SELECT * FROM news_articles WHERE "+filter+" ORDER BY CASE placement WHEN 'LEAD' THEN 0 WHEN 'SIDEBAR_1' THEN 1 WHEN 'SIDEBAR_2' THEN 2 ELSE 3 END, publish_at DESC NULLS LAST, updated_at DESC LIMIT 100",(rs,n)->map(rs));
    }
    public NewsArticle find(UUID id, boolean admin) {
        return db.query("SELECT * FROM news_articles WHERE id=?" + (admin ? "" : " AND " + PUBLIC),
            (rs,n) -> map(rs), id).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private NewsArticle map(java.sql.ResultSet r) throws java.sql.SQLException {
        UUID id = r.getObject("id", UUID.class);
        var corrections = db.query("SELECT note,created_at FROM news_corrections WHERE article_id=? ORDER BY created_at",
            (rs,n) -> new NewsArticle.Correction(rs.getString(1), rs.getObject(2,OffsetDateTime.class)), id);
        OffsetDateTime publishedAt=r.getObject("publish_at",OffsetDateTime.class);
        String status=r.getString("status");
        if ("SCHEDULED".equals(status) && publishedAt!=null && !publishedAt.isAfter(OffsetDateTime.now())) status="PUBLISHED";
        return new NewsArticle(id,r.getString("title"),r.getString("category"),r.getString("summary"),r.getString("body"),r.getString("author"),r.getObject("image_id",UUID.class),r.getString("image_alt"),r.getString("image_caption"),r.getString("image_credit"),r.getString("sources"),status,publishedAt,r.getBoolean("is_lead"),r.getInt("version"),r.getObject("updated_at",OffsetDateTime.class),corrections,r.getString("placement"),r.getObject("deleted_at",OffsetDateTime.class));
    }
    @Transactional
    public NewsArticle save(UUID id, NewsArticle a, String actor, boolean create) {
        // Serialize saves, including lead changes, and prevent stale tabs from overwriting updates.
        db.execute("LOCK TABLE news_articles IN SHARE ROW EXCLUSIVE MODE");
        if (a.imageId()!=null && Boolean.FALSE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM news_images WHERE id=?)",Boolean.class,a.imageId())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload an image first");
        if (!create && find(id,true).deletedAt()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Restore this article from trash before editing");
        if (!create && find(id,true).version()!=a.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Article changed. Reload before saving.");
        String placement=a.placement()==null?(a.isLead()?"LEAD":"MORE"):a.placement();
        if(!Set.of("LEAD","SIDEBAR_1","SIDEBAR_2","MORE").contains(placement))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid homepage placement");
        if("SCHEDULED".equals(a.status()) && !"MORE".equals(placement))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Scheduled articles start in More stories. Choose a featured slot after publication.");
        if("PUBLISHED".equals(a.status()) && !"MORE".equals(placement)) {
            var displaced=db.queryForList("SELECT id FROM news_articles WHERE placement=? AND status='PUBLISHED' AND deleted_at IS NULL AND id<>?",UUID.class,placement,id);
            db.update("UPDATE news_articles SET placement='MORE',is_lead=false,version=version+1,updated_at=now() WHERE placement=? AND status='PUBLISHED' AND deleted_at IS NULL AND id<>?",placement,id);
            for(UUID previous:displaced) audit(previous,actor,"MOVED_TO_MORE_STORIES",find(previous,true).version());
        }
        boolean lead="LEAD".equals(placement);
        OffsetDateTime date = "DRAFT".equals(a.status()) ? null : ("PUBLISHED".equals(a.status()) && a.publishAt()==null ? OffsetDateTime.now() : a.publishAt());
        if ("PUBLISHED".equals(a.status()) && date!=null && date.isAfter(OffsetDateTime.now())) date=OffsetDateTime.now();
        if ("SCHEDULED".equals(a.status()) && (date==null || !date.isAfter(OffsetDateTime.now()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a future publication time");
        if (!"DRAFT".equals(a.status()) && a.imageId()!=null && (a.imageAlt().isBlank() || a.imageCredit().isBlank())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image alt text and credit are required for publication");
        if (create) db.update("INSERT INTO news_articles(id,title,category,summary,body,author,image_id,image_alt,image_caption,image_credit,sources,status,publish_at,is_lead,placement) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,a.title(),a.category(),a.summary(),a.body(),a.author(),a.imageId(),a.imageAlt(),a.imageCaption(),a.imageCredit(),a.sources(),a.status(),date,lead,placement);
        else db.update("UPDATE news_articles SET title=?,category=?,summary=?,body=?,author=?,image_id=?,image_alt=?,image_caption=?,image_credit=?,sources=?,status=?,publish_at=?,is_lead=?,placement=?,version=version+1,updated_at=now() WHERE id=?",a.title(),a.category(),a.summary(),a.body(),a.author(),a.imageId(),a.imageAlt(),a.imageCaption(),a.imageCredit(),a.sources(),a.status(),date,lead,placement,id);
        var saved=find(id,true);
        audit(id,actor,"SAVE_"+a.status(),saved.version());
        return saved;
    }
    @Transactional
    public void correct(UUID id, String note, String actor) {
        find(id,true);
        db.update("INSERT INTO news_corrections(id,article_id,note) VALUES (?,?,?)",UUID.randomUUID(),id,note);
        db.update("UPDATE news_articles SET version=version+1,updated_at=now() WHERE id=?",id);
        audit(id,actor,"CORRECTION",find(id,true).version());
    }
    @Transactional
    public NewsArticle trash(UUID id,int version,String actor,boolean restore) {
        db.execute("LOCK TABLE news_articles IN SHARE ROW EXCLUSIVE MODE");
        var current=find(id,true);
        if(current.version()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Article changed. Reload before changing trash status.");
        if(restore && current.deletedAt()==null || !restore && current.deletedAt()!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Article trash status already changed.");
        db.update("UPDATE news_articles SET deleted_at="+(restore?"NULL":"now()")+",status='DRAFT',publish_at=NULL,placement='MORE',is_lead=false,version=version+1,updated_at=now() WHERE id=?",id);
        var saved=find(id,true);audit(id,actor,restore?"RESTORED_AS_DRAFT":"MOVED_TO_TRASH",saved.version());return saved;
    }
    private void audit(UUID id,String actor,String action,int version) {
        db.update("INSERT INTO news_audit(id,article_id,actor_sub,action,version) VALUES (?,?,?,?,?)",UUID.randomUUID(),id,actor,action,version);
    }
    public UUID image(String type,byte[] bytes) {
        UUID id=UUID.randomUUID(); db.update("INSERT INTO news_images(id,content_type,data) VALUES (?,?,?)",id,type,bytes); return id;
    }
    public Image image(UUID id, boolean admin) {
        String visible=admin ? "" : " AND EXISTS(SELECT 1 FROM news_articles WHERE image_id=news_images.id AND "+PUBLIC+")";
        return db.query("SELECT content_type,data FROM news_images WHERE id=?"+visible,(rs,n)->new Image(rs.getString(1),rs.getBytes(2)),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public record Image(String type,byte[] bytes) {}
}
