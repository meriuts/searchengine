package searchengine.services.siteparser;

import org.springframework.stereotype.Component;

@Component
public class LinkCollectorFactory {
    private final PageNodeFactory pageNodeFactory;

    public LinkCollectorFactory(PageNodeFactory pageNodeFactory) {
        this.pageNodeFactory = pageNodeFactory;
    }

    public LinkCollector createLinkCollector(String url) {
        return new LinkCollector(url, pageNodeFactory);
    }
}
