package searchengine.services.siteparser;

import java.util.concurrent.RecursiveAction;

public class LinkCollector extends RecursiveAction {
    private final String url;
    private final PageNodeFactory pageNodeFactory;

    public LinkCollector(String url, PageNodeFactory pageNodeFactory) {
        this.url = url;
        this.pageNodeFactory = pageNodeFactory;
    }
    @Override
    protected void compute() {
        PageNode pageNode = pageNodeFactory.createPageNode(url);
        pageNode.parsePage();

        for (String childUrl: pageNode.getChildUrls()) {
            LinkCollector task = new LinkCollector(childUrl, pageNodeFactory);
            task.fork();
        }
    }


}
