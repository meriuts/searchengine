package searchengine.services.siteparser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PageParser {

    @Cacheable(value = "myCache", key = "#url", sync = true)
    public String parsePage(String url) {
        return null;

    }


}
