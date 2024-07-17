package searchengine.services.siteparser;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexErrorResponse;
import searchengine.dto.index.IndexResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.contentparser.LemmaFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheManager = "redisCacheManager")
public class PageParser {
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Cacheable(value = "parsedUrl", key = "#url")
    public Set<String> startParsing(String url) {

        try {
            Thread.sleep(200);
            System.out.println("parse " + url);

            String path = new URL(url).getPath();
            SiteEntity siteEntity = siteRepository.findByUrl(new URL(url).getHost());
            if (!pageRepository.findByPathAndSiteId(path, siteEntity).isEmpty()) {
                return new HashSet<>();
            }

            Response response = executePageInfo(url);
            Document content = response.parse();
            PageEntity pageEntity = PageEntity.mapToPageEntity(siteEntity, response, content);

            savePage(pageEntity);

            return findUrls(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public IndexResponse startParsingOnePage(String url) {
        String urlFormat = "https?://[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)";
        if (!url.matches(urlFormat)) {
            return new IndexErrorResponse("Указан неверный формат ссылки (required: http(s)://)");
        }

        if (!linkIsValid(url)) {
            return new IndexErrorResponse("Страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        try {
            String path = new URL(url).getPath();
            SiteEntity siteEntity = siteRepository.findByUrl(new URL(url).getHost());
            if (siteEntity == null) {
                return new IndexErrorResponse("Сайт не был индексирован");
            }

            Response response = executePageInfo(url);
            Document content = response.parse();
            PageEntity pageEntity = PageEntity.mapToPageEntity(siteEntity, response, content);
            Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(pageEntity.getPageContent());
            save(pageEntity, lemmas);

            return new IndexResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Transactional
    private void save(PageEntity pageEntity, Map<String, Integer> lemmas) {
        PageEntity page = savePage(pageEntity);
        List<LemmaEntity> lemmaEntityList = saveLemmas(page, lemmas);
        saveIndexes(page, lemmaEntityList, lemmas);
    }

    @Transactional
    private PageEntity savePage(PageEntity pageEntity) {
        Optional<PageEntity> existingPage = pageRepository.findByPathAndSiteId(pageEntity.getPath(), pageEntity.getSiteId());
        if (existingPage.isPresent()) {
            return existingPage.get();
        }
        return pageRepository.save(pageEntity);
    }

    @Transactional
    private List<LemmaEntity> saveLemmas (PageEntity pageEntity, Map<String, Integer> lemmas) {
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        for (Map.Entry<String, Integer> lemmaEntry: lemmas.entrySet()) {
            Optional<LemmaEntity> existingLemma =
                    lemmaRepository.findByLemmaAndSiteId(lemmaEntry.getKey(), pageEntity.getSiteId());
            if (existingLemma.isPresent()) {
                LemmaEntity lemmaEntity = existingLemma.get();
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
                lemmaEntityList.add(lemmaEntity);
            } else {
                LemmaEntity lemmaEntity = new LemmaEntity();
                lemmaEntity.setSiteId(pageEntity.getSiteId());
                lemmaEntity.setLemma(lemmaEntry.getKey());
                lemmaEntity.setFrequency(1);
                lemmaEntityList.add(lemmaEntity);
            }
        }
        return lemmaRepository.saveAll(lemmaEntityList);
    }

    @Transactional
    private List<IndexEntity> saveIndexes(PageEntity pageEntity, List<LemmaEntity> lemmaEntities, Map<String, Integer> lemmas) {
        List<IndexEntity> indexEntityList = new ArrayList<>();
        for (LemmaEntity lemmaEntity : lemmaEntities) {
            Integer lemmaRank = lemmas.get(lemmaEntity.getLemma());
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setPageId(pageEntity);
            indexEntity.setLemmaId(lemmaEntity);
            indexEntity.setRank(lemmaRank);
            indexEntityList.add(indexEntity);
        }
        return indexRepository.saveAll(indexEntityList);
    }

    private Response executePageInfo(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                .referrer("https://ya.ru/")
                .execute();
    }


    private Set<String> findUrls(Document content) {
        Elements elementsWithUrl = content.select("a[href]");
        Set<String> uniqueValidUrl = elementsWithUrl.stream()
                .map(element -> element.absUrl("href"))
                .filter(this::linkIsValid)
                .collect(Collectors.toSet());
        return uniqueValidUrl;
    }

    private boolean linkIsValid(String link) {
        List<String> urls = sites.getSites().stream().map(Site::getUrl).toList();
        try {
            for (String url : urls) {
                String host = new URL(url).getHost();
                String urlFormatRegex = String.format("https?://%s[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)", host);
                if (link.matches(urlFormatRegex)) return true;
            }
            return false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


}
