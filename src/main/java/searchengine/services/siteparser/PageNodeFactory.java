package searchengine.services.siteparser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.repositories.LinkHashRepository;
import searchengine.repositories.PageHashRepository;

@Component
public class PageNodeFactory {
    private final PageHashRepository pageHashRepository;
    private final LinkHashRepository linkHashRepository;

    @Autowired
    public PageNodeFactory(PageHashRepository pageHashRepository, LinkHashRepository linkHashRepository) {
        this.pageHashRepository = pageHashRepository;
        this.linkHashRepository = linkHashRepository;
    }

    public PageNode createPageNode(String url) {
        return new PageNode(url, pageHashRepository, linkHashRepository);
    }

}
