package searchengine.services.siteparser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PageNodeFactory {

    @Autowired
    public PageNodeFactory() {
    }


    public PageNode createPageNode(String url) {
        return new PageNode(url);
    }

}
