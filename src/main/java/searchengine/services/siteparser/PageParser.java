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
import searchengine.repositories.*;
import searchengine.services.contentparser.LemmaFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheManager = "redisCacheManager")
public class PageParser {
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepositoryNative lemmaRepositoryNative;

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

            if (pageRepository.findByPathAndSiteId(path, siteEntity).isPresent()) {
                return new IndexErrorResponse("Страница сайта уже проиндексирована");
            }

            Response response = executePageInfo(url);
            Document content = response.parse();
            PageEntity pageEntity = PageEntity.mapToPageEntity(siteEntity, response, content);

            Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(pageEntity.getPageContent());
            List<Map<String, Integer>> lemmaPartList = getLemmaPartList(lemmas);

            save(pageEntity, lemmaPartList);

            return new IndexResponse();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //каскадно не удаляются записи когда удаляем страницу page

    @Transactional
    private void save(PageEntity pageEntity, List<Map<String, Integer>> lemmaPartList) {
        PageEntity page = pageRepository.save(pageEntity); // страница все равно сохраняется даже если потом на леммах падает
        lemmaPartList.parallelStream().forEach(lemmaPart -> saveLemmaAndIndex(page, lemmaPart));
    }

    @Transactional
    private void saveLemmaAndIndex(PageEntity pageEntity, Map<String, Integer> lemmaPartsList) {
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();

        for (Map.Entry<String, Integer> lemmaEntry : lemmaPartsList.entrySet()) {
            LemmaEntity lemmaEntity = LemmaEntity.getLemmaEntity(pageEntity.getSiteId(), lemmaEntry.getKey());
            IndexEntity indexEntity = new IndexEntity(pageEntity, lemmaEntity, lemmaEntry.getValue());
            lemmaEntity.getIndexEntityList().add(indexEntity);
            lemmaEntityList.add(lemmaEntity);
        }

        List<LemmaEntity> existingLemmaEntities = lemmaRepositoryNative.findAllByLemmaInAndSiteIdForUpdate(
                lemmaPartsList.keySet(),
                pageEntity.getSiteId().getId()
        );

        lemmaEntityList.removeAll(existingLemmaEntities);

        for (LemmaEntity existingLemma : existingLemmaEntities) {
            existingLemma.setFrequency(existingLemma.getFrequency() + 1);
            IndexEntity indexEntity = new IndexEntity(
                    pageEntity,
                    existingLemma,
                    lemmaPartsList.get(existingLemma.getLemma())
            );
            existingLemma.getIndexEntityList().add(indexEntity);
        }

        lemmaRepository.saveAll(lemmaEntityList);
        lemmaRepository.saveAll(existingLemmaEntities);
    }

    @Transactional
    private PageEntity savePage(PageEntity pageEntity) {
        Optional<PageEntity> existingPage = pageRepository.findByPathAndSiteId(pageEntity.getPath(), pageEntity.getSiteId());
        if (existingPage.isPresent()) {
            return existingPage.get();
        }
        return pageRepository.save(pageEntity);
    }

    private List<Map<String, Integer>> getLemmaPartList(Map<String, Integer> lemmas) {
        int part = 20;
        int total = lemmas.entrySet().size();
        List<Map.Entry<String, Integer>> entryLemmaList = lemmas.entrySet().stream().toList();
        List<Map<String, Integer>> lemmaPartList = IntStream
                .range(0, (total + part - 1) / part)
                .mapToObj(i -> entryLemmaList.subList(i * part, Math.min(total, (i + 1) * part)))
                .map(subList -> subList.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
        return lemmaPartList;
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
