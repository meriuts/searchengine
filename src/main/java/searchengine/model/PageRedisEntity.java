package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Map;


@Data
@RequiredArgsConstructor
@RedisHash("pages")
public class PageRedisEntity implements Serializable {
    @Id
    private String pageRedisId;
    private Integer pageId;
    private SiteEntity siteId;
    private String path;
    private Integer statusCode;
    private String pageContent;
    private Map<String, Integer> lemmas;

    public static PageRedisEntity mapToPageMessage(PageEntity pageEntity) {
        PageRedisEntity pageRedisEntity = new PageRedisEntity();
        pageRedisEntity.setPageId(pageEntity.getId());
        pageRedisEntity.setSiteId(pageEntity.getSiteId());
        pageRedisEntity.setPath(pageEntity.getPath());
        pageRedisEntity.setStatusCode(pageEntity.getStatusCode());
        pageRedisEntity.setPageContent(pageEntity.getPageContent());
        return pageRedisEntity;
    }
}
