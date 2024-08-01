package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import searchengine.model.IndexEntity;
import searchengine.services.contentparser.LemmaFinder;

import java.util.List;
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
        searchData.setTitle(indexEntity.getPageId().getPageTitle());
        searchData.setSnippet(getSnippet(indexEntity.getPageId().getPageContent(), query));
        searchData.setAbsRelevance(indexEntity.getRank() * 1.0);
        return searchData;
    }


    private static String getSnippet(String content, String query) {
        List<String> words = LemmaFinder.getInstance().getWords(query);
        int startWordIndex = content.indexOf(words.get(0));
        String subContent = content.substring(
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

        return snippet.toString();
    }
}
