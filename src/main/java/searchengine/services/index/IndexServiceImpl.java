package searchengine.services.index;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexErrorResponse;
import searchengine.dto.index.IndexRequest;
import searchengine.dto.index.IndexResponse;
import searchengine.exception.ParsingException;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.siteparser.LinkCollector;
import searchengine.services.siteparser.PageNodeFactory;
import searchengine.services.siteparser.PageParser;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;
    private final PageNodeFactory pageNodeFactory;
    private final PageParser pageParser;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private ForkJoinPool pool;

    private ForkJoinPool getPoolInstance() {
        if (pool == null || pool.isShutdown()) {
            pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        }
        return pool;
    }

    @Override
    public IndexResponse startIndexing() {
        pageParser.getIsStopped().set(false);
        pool = getPoolInstance();
        if (pool.getActiveThreadCount() > 0) {
            return new IndexErrorResponse("Индексация уже запущена");
        }

        cleanDataInAllRepository();

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
            pool.shutdown();
            pool.shutdownNow();
            try {
                pool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            pageParser.getIsStopped().set(true);

            for (SiteEntity siteEntity : siteRepository.findAll()) {
                if (siteEntity.getStatus() != SiteStatus.INDEXED) {
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
        try {
            pageParser.startParsingOnePage(url.getUrl());
        } catch (ParsingException e) {
            return new IndexErrorResponse(e.getMessage());
        }
        return new IndexResponse();
    }

    @Transactional
    private void cleanDataInAllRepository() {
        indexRepository.cleanTable();
        lemmaRepository.cleanTable();
        pageRepository.cleanTable();
        siteRepository.deleteAll();
    }
}
