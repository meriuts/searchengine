package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexResponse;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.siteparser.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;
    private final PageNodeFactory pageNodeFactory;
    private final LinkCollectorFactory linkCollectorFactory;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;



    public void test(String t) {
        System.out.println("вызвали метод " + t);
    }

    public IndexResponse startIndexing() {


        final ExecutorService executorServiceParser = Executors.newFixedThreadPool(3);
        sites.getSites().forEach(site ->
                executorServiceParser.submit(() -> startParsing(site))
        );

        return null;
    }
    private void startParsing(Site site) {
        SiteEntity siteEntity = mapToSiteEntity(site);
        siteRepository.save(siteEntity);

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(new LinkCollector(siteEntity.getUrl(), pageNodeFactory));


//        PageNode page = new PageNode(siteEntity.getUrl());
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        forkJoinPool.invoke(new LinkCollector(page, pageRepository));

//        PageNode rootPageNode = new PageNode(siteEntity.getUrl());
//        rootPageNode.parseAllPage();
//        Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(rootPageNode.getContent().text());
//        rootPageNode.setLemmas(lemmas);
//        ForkJoinPool forkJoinPool = new ForkJoinPool();
//        forkJoinPool.invoke(new CollectorLinks(rootPageNode));
//        while (!exit) {}
//        forkJoinPool.shutdownNow();
    }

    private SiteEntity mapToSiteEntity(Site site) {
       return SiteEntity.builder()
                .status(SiteStatus.INDEXING)
                .statusTime(LocalDateTime.now())
                .url(site.getUrl())
                .name(site.getName())
                .build();
    }

    private PageEntity mapToPageEntity(PageNode page) {
        return PageEntity.builder()
                .path(page.getUrl())
                .build();

    }

}
