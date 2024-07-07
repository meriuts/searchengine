package searchengine.services.siteparser;

import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.LinkHashEntity;
import searchengine.model.PageHashEntity;
import searchengine.repositories.LinkHashRepository;
import searchengine.repositories.PageHashRepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class PageNode {
    private String url;
    private Set<String> childUrls;
    private final PageHashRepository pageHashRepository;
    private final LinkHashRepository linkHashRepository;

    public PageNode(String url, PageHashRepository pageHashRepository, LinkHashRepository linkHashRepository) {
        this.url = url;
        this.childUrls = new HashSet<>();
        this.pageHashRepository = pageHashRepository;
        this.linkHashRepository = linkHashRepository;
    }

    public void addChildUrl(String childUrl) {
        childUrls.add(childUrl);
    }

    public void parsePage() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (isLinkParse(url)) return;

        try {
            System.out.println("parse " + url);
            Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .execute();

            Document content = response.parse();
            findUrls(content).forEach(this::addChildUrl);

            PageHashEntity pageHashEntity = new PageHashEntity(
                    UUID.randomUUID().toString(),
                    url,
                    response.statusCode(),
                    content.text(),
                    childUrls);
//            pageHashRepository.save(pageHashEntity);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLinkParse(String url) {
        if (linkHashRepository.existsById(url)) return true;
        linkHashRepository.save(new LinkHashEntity(url));
        return false;
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
