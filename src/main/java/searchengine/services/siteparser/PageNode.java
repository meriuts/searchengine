package searchengine.services.siteparser;

import lombok.Data;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PageNode {
    private String url;
    private Set<String> childUrls;

    public PageNode(String url) {
        this.url = url;
        this.childUrls = new HashSet<>();
    }

    public void addChildUrl(String childUrl) {
        childUrls.add(childUrl);
    }


    @Cacheable(value = "myCache", key = "#url", sync = true)
    public String parsePage(String url) {
        try {
            Thread.sleep(200);
            System.out.println("parse " + url);
            Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .execute();
            Document content = response.parse();
            findUrls(content).forEach(this::addChildUrl);
            return url;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
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
        try {
            String host = new URL(url).getHost();
            String urlFormatRegex = String.format("https?://%s[^,\\s?]+(?<!\\.(?:jpg|png|gif|pdf))(?<!#)", host);
            return link.matches(urlFormatRegex);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
