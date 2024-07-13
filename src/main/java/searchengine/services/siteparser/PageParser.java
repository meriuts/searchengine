package searchengine.services.siteparser;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheManager = "redisCacheManager")
public class PageParser {
    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;



    @Cacheable(value = "parsedUrl", key = "#url")
    public Set<String> startParsing(String url) {
        try {
            Thread.sleep(200);
            System.out.println("parse " + url);
            Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .referrer("https://ya.ru/")
                    .execute();
            Document content = response.parse();

            PageEntity pageEntity = new PageEntity();
            pageEntity.setSiteId(siteRepository.findByUrl(new URL(url).getHost()));
            pageEntity.setPath(new URL(url).getPath());
            pageEntity.setStatusCode(response.statusCode());
            pageEntity.setPageContent(content.text());

            pageRepository.save(pageEntity);

            return findUrls(content);
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
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
            for(String url: urls) {
                String host = new URL(url).getHost();
                String urlFormatRegex = String.format("https?://%s[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)", host);
                if(link.matches(urlFormatRegex)) return true;
            }
            return false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


}
