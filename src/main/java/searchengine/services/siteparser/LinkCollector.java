package searchengine.services.siteparser;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LinkCollector extends RecursiveAction {
    private final String url;
    private final PageNodeFactory pageNodeFactory;



    public LinkCollector(String url, PageNodeFactory pageNodeFactory) {
        this.url = url;
        this.pageNodeFactory = pageNodeFactory;
    }
    @Override
    protected void compute() {
//        System.out.printf("Task %s execute in thread %s%n", this, Thread.currentThread().getName());
        PageNode pageNode = pageNodeFactory.createPageNode(url);
        pageNode.parsePage();


        for (String childUrl: pageNode.getChildUrls()) {
            LinkCollector task = new LinkCollector(childUrl, pageNodeFactory);
            task.fork();
        }
    }


}
