package app.sanctuary.api.news;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record NewsArticle(
    UUID id,
    @NotBlank @Size(max=200) String title,
    @NotNull @Pattern(regexp="Vatican|U\\.S\\. Church") String category,
    @NotBlank @Size(max=600) String summary,
    @NotBlank @Size(max=100000) String body,
    @NotBlank @Size(max=150) String author,
    UUID imageId,
    @NotNull @Size(max=300) String imageAlt,
    @NotNull @Size(max=1000) String imageCaption,
    @NotNull @Size(max=500) String imageCredit,
    @NotNull @Size(max=10000) String sources,
    @NotNull @Pattern(regexp="DRAFT|PUBLISHED|SCHEDULED") String status,
    OffsetDateTime publishAt,
    boolean isLead,
    @Min(0) int version,
    OffsetDateTime updatedAt,
    List<Correction> corrections,
    @Pattern(regexp="LEAD|SIDEBAR_1|SIDEBAR_2|MORE") String placement,
    OffsetDateTime deletedAt
) {
    // Compatibility for existing callers while homepage placement becomes explicit.
    public NewsArticle(UUID id,String title,String category,String summary,String body,String author,UUID imageId,String imageAlt,String imageCaption,String imageCredit,String sources,String status,OffsetDateTime publishAt,boolean isLead,int version,OffsetDateTime updatedAt,List<Correction> corrections){
        this(id,title,category,summary,body,author,imageId,imageAlt,imageCaption,imageCredit,sources,status,publishAt,isLead,version,updatedAt,corrections,isLead?"LEAD":"MORE",null);
    }
    public record Correction(String note, OffsetDateTime createdAt) {}
}
