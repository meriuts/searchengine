package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexErrorResponse;
import searchengine.dto.index.IndexRequest;
import searchengine.dto.index.IndexResponse;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.siteparser.LinkCollector;
import searchengine.services.siteparser.PageNodeFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;
    private final PageNodeFactory pageNodeFactory;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private ForkJoinPool pool;

    private ForkJoinPool getPoolInstance() {
        if(pool == null || pool.isShutdown()) {
            pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        }
        return pool;
    }

    @Override
    @CacheEvict(value = {"parsedUrl", "page", "site"}, allEntries = true)
    public IndexResponse startIndexing() {
        pool = getPoolInstance();
        if (pool.getActiveThreadCount() > 0) {
            return new IndexErrorResponse("Индексация уже запущена");
        }
        siteRepository.deleteAll();

        sites.getSites().forEach(site -> {
            SiteEntity siteEntity = SiteEntity.mapToSiteEntity(site, SiteStatus.INDEXING);
            siteRepository.save(siteEntity);
            pool.execute(new LinkCollector(site.getUrl(), pageNodeFactory));
        });

        return new IndexResponse();
    }

    @Override
    public IndexResponse stopIndexing() {
        pool = getPoolInstance();
        if (pool.getActiveThreadCount() > 0) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            for (SiteEntity siteEntity : siteRepository.findAll()) {
                if(siteEntity.getStatus() != SiteStatus.INDEXED) {
                    siteEntity.setErrorText("Индкесация остановлена пользователем");
                    siteEntity.setStatus(SiteStatus.FAILED);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteEntity);
                }
            }
                return new IndexResponse();
        }
        return new IndexErrorResponse("Индексация не запущена");
    }

    @Override
    public IndexResponse indexPage(IndexRequest url) {
        pageNodeFactory
                .createPageNode(url.getUrl())
                .parsePage(url.getUrl());

        return new IndexResponse();
    }





}
