package searchengine.services.siteparser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.exception.ParsingException;
import searchengine.exception.StopTaskException;
import searchengine.message.RedisMessagePublisher;
import searchengine.model.*;
import searchengine.repositories.*;
import searchengine.services.contentparser.LemmaFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Getter
@Setter
@RequiredArgsConstructor
@CacheConfig(cacheManager = "redisCacheManager")
public class PageParser {
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepositoryNative lemmaRepositoryNative;
    private final IndexRepositoryNative indexRepositoryNative;
    private final RedisRepository redisRepository;
    private final RedisRepositoryImpl redisRepositoryImpl;
    private final RedisMessagePublisher redisMessagePublisher;
    private final String urlFormat = "https?://[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)";
    private AtomicBoolean isStopped = new AtomicBoolean(false);

    @Cacheable(value = "parsedUrl", key = "#url")
    public Set<String> startParsing(String url) {
        try {
            if (isStopped.get()) {
                throw new StopTaskException("Выполнение задач парсинга остановлено пользователем");
            }
            Thread.sleep(200);
            System.out.println("parse " + url);

            return startParsingOnePage(url);
        } catch (Exception ex) {
            return new HashSet<>();
        }
    }

    public Set<String> startParsingOnePage(String url) throws ParsingException {
        if (!url.matches(urlFormat)) {
            throw new ParsingException("Указан неверный формат ссылки");
        }

        if (!linkIsValid(url)) {
            throw new ParsingException("Страница находится за пределами сайтов, указанных в конфигурационном файле");
        }

        try {
            SiteEntity siteEntity = siteRepository.findByUrl(new URL(url).getHost());
            if (siteEntity == null) {
                throw new ParsingException("Сайт не был индексирован");
            }

            PageEntity p = pageRepository.findByPathAndSiteId(new URL(url).getPath(), siteEntity);
            if (p != null) {
                throw new ParsingException("Страница проиндексирован");
            }

            Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .referrer("https://ya.ru/")
                    .execute();

            Document content = response.parse();
            PageEntity pageEntity = PageEntity.mapToPageEntity(siteEntity, response, content);
            PageEntity page = savePage(pageEntity);
            Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(pageEntity.getPageContent());

            PageRedisEntity pageRedisEntity = PageRedisEntity.mapToPageMessage(page);
            pageRedisEntity.setLemmas(lemmas);
            PageRedisEntity savePageRedisEntity = redisRepository.save(pageRedisEntity);

            redisMessagePublisher.publish(savePageRedisEntity.getPageRedisId());



            return findUrls(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void initSaveLemmaAndIndex(String pageKey) {
        PageRedisEntity pageRedisEntity = redisRepository.findById(pageKey).orElseThrow();

        for (Map.Entry<String, Integer> lemmaEntry : pageRedisEntity.getLemmas().entrySet()) {
                saveLemmaAndIndex(pageRedisEntity, lemmaEntry);
        }

    }


    @Transactional
    private void saveLemmaAndIndex( PageRedisEntity pageRedisEntity, Map.Entry<String, Integer> lemma) {
        LemmaEntity lemmaEntity = LemmaEntity.getLemmaEntity(pageRedisEntity.getSiteId(), lemma.getKey());
        LemmaEntity existLemmaEntity = lemmaRepository.findByLemmaAndSiteId(lemma.getKey(), pageRedisEntity.getSiteId());
        if(existLemmaEntity != null) {
            existLemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            lemmaEntity = existLemmaEntity;
        }

        LemmaEntity savedLemma = saveLemma(lemmaEntity);

//        IndexEntity indexEntity = new IndexEntity(pageRedisEntity, savedLemma, lemma.getValue());
//        indexRepository.save(indexEntity);
    }

    @CachePut(value = "parsedUrl", key = "#pageEntity.path + ':' + #pageEntity.siteId.id", unless = "#result == null")
    private PageEntity savePage(PageEntity pageEntity) {
        return pageRepository.save(pageEntity);
    }

    @CachePut(value = "lemma", key = "#lemmaEntity.lemma + ':' + #lemmaEntity.siteId.id", unless = "#result == null")
    private LemmaEntity saveLemma(LemmaEntity lemmaEntity) {
        return lemmaRepository.save(lemmaEntity);
    }


    private List<Map<String, Integer>> getLemmaPartList(Map<String, Integer> lemmas) {
        int part = 10;
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
                String urlFormatRegex = String.format("https?://%s[^,\\s?]*(?<!\\.(?:jpg|png|gif|pdf))(?<!#)", host);
                if (link.matches(urlFormatRegex)) return true;
            }
            return false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
