package searchengine.services.siteparser;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.dto.index.IndexResponse;

import java.util.HashSet;
import java.util.Set;

@Data
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PageNode {
    private String url;
    private Set<String> childUrls;
    private PageParser pageParser;
    private CacheManager redisCacheManager;

    @Autowired
    public PageNode(String url, PageParser pageParser, CacheManager redisCacheManager) {
        this.url = url;
        this.childUrls = new HashSet<>();
        this.pageParser = pageParser;
        this.redisCacheManager = redisCacheManager;
    }

    public void parsePage() {
//        Cache cache = redisCacheManager.getCache("parsedUrl");
//        if (cache != null && cache.get(url) != null) {
//            return;
//        }
        childUrls.addAll(pageParser.startParsing(url));
    }
}
