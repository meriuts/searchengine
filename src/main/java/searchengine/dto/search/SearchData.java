package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.annotation.Transient;
import searchengine.model.IndexEntity;
import searchengine.services.contentparser.LemmaFinder;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
public class SearchData {
    @Transient
    private Integer pageId;
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double absRelevance;
    private Double relevance;

    public static SearchData mapToSearchData(IndexEntity indexEntity, String query) {
        SearchData searchData = new SearchData();
        searchData.setPageId(indexEntity.getPageId().getId());
        searchData.setSite(indexEntity.getLemmaId().getSiteId().getUrl());
        searchData.setSiteName(indexEntity.getLemmaId().getSiteId().getName());
        searchData.setUri(indexEntity.getPageId().getPath());

        Map<String, String> titleAndSnippet = getSnippet(indexEntity.getPageId().getPageContent(), query);
        searchData.setTitle(titleAndSnippet.keySet().iterator().next());
        searchData.setSnippet(titleAndSnippet.values().iterator().next());
        searchData.setAbsRelevance(indexEntity.getRank() * 1.0);
        return searchData;
    }


    private static  Map<String, String> getSnippet(String content, String query) {
        Map<String, String> titleAndSnippet = new HashMap<>();

        int startTitle = content.indexOf("<title>") + "<title>".length();
        int endTitle = content.indexOf("</title>");
        String title = content.substring(Math.max(0, startTitle), Math.min(endTitle, content.length()));


        String cleanContent = Jsoup.parse(content).text();

        List<String> words = LemmaFinder.getInstance().getWords(query);
        int startWordIndex = cleanContent.indexOf(words.get(0));

        String subContent = cleanContent.substring(
                Math.max(0, startWordIndex - 200),
                Math.min(content.length(), startWordIndex + 200));

        String regexWords = "(" + String.join("|", words) + ")";
        Pattern pattern = Pattern.compile(regexWords, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(subContent);

        StringBuilder snippet = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(snippet, "<b>" + matcher.group() + "</b>");
        }
            matcher.appendTail(snippet);

        titleAndSnippet.put(title, snippet.toString());

        return titleAndSnippet;
    }
}
