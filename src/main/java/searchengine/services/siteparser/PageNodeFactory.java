package searchengine.services.siteparser;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PageNodeFactory {
    private final PageParser pageParser;
    private final CacheManager cacheManager;

    public PageNode createPageNode(String url) {
        return new PageNode(url, pageParser, cacheManager);
    }

}
