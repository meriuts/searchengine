package searchengine.services.siteparser;

import com.sun.xml.bind.v2.TODO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexErrorResponse;
import searchengine.dto.index.IndexResponse;
import searchengine.exception.ParsingException;
import searchengine.exception.StopTaskException;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.*;
import searchengine.services.contentparser.LemmaFinder;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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
    private final String urlFormat = "https?://[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)";
    private AtomicBoolean isStopped = new AtomicBoolean(false);

//    @Cacheable(value = "parsedUrl", key = "#url")
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
            String path = new URL(url).getPath();
            SiteEntity siteEntity = siteRepository.findByUrl(new URL(url).getHost());
            if (siteEntity == null) {
                throw new ParsingException("Сайт не был индексирован");
            }

            Response response = executePageInfo(url);
            Document content = response.parse();
            PageEntity pageEntity = PageEntity.mapToPageEntity(siteEntity, response, content);

            Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(pageEntity.getPageContent());
            List<Map<String, Integer>> lemmaPartList = getLemmaPartList(lemmas);

            save(pageEntity, lemmaPartList);

            return findUrls(content);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Transactional
    private void save(PageEntity pageEntity, List<Map<String, Integer>> lemmaPartList) {
        PageEntity page = pageRepository.save(pageEntity);
        for (Map<String, Integer> lemmaPart : lemmaPartList) {
            saveLemmaAndIndex(page, lemmaPart);
        }
//        lemmaPartList.parallelStream().forEach(lemmaPart -> saveLemmaAndIndex(page, lemmaPart));
    }


    @Transactional
    private void saveLemmaAndIndex(PageEntity pageEntity, Map<String, Integer> lemmaPart) {
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        List<IndexEntity> indsexEntityList = new ArrayList<>();
        for (Map.Entry<String, Integer> lemmaEntry : lemmaPart.entrySet()) {
            LemmaEntity lemmaEntity = LemmaEntity.getLemmaEntity(pageEntity.getSiteId(), lemmaEntry.getKey());
            // на этот метод кэш нужен - нужно правильно настроить
            // сейчас сохраняется много дуюликатов лемм - нужно поставить ограничения на таблицы индекс и леммы
            //это долгая операция - много лемм потому что - подумать как оптимизировать - прям очень долго получается
            Optional<LemmaEntity> existingLemma =
                    lemmaRepository.findByLemmaAndSiteId(lemmaEntry.getKey(), pageEntity.getSiteId());
            if(existingLemma.isPresent()) {
                lemmaEntity = existingLemma.get();
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            lemmaEntityList.add(lemmaEntity);

            IndexEntity indexEntity = new IndexEntity(pageEntity, lemmaEntity, lemmaEntry.getValue());
            indsexEntityList.add(indexEntity);
        }
        lemmaRepository.saveAll(lemmaEntityList);
        indexRepositoryNative.insertBatchIndex(indsexEntityList);
//        indexRepository.saveAll(indsexEntityList);
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
