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
    private Integer absRelevance;
    private Double relevance;

    public static SearchData mapToSearchData(IndexEntity indexEntity, String query) {
        SearchData searchData = new SearchData();
        searchData.setPageId(indexEntity.getPageId().getId());
        searchData.setSite(indexEntity.getLemmaId().getSiteId().getUrl());
        searchData.setSiteName(indexEntity.getLemmaId().getSiteId().getName());
        searchData.setUri(indexEntity.getPageId().getPath());
        searchData.setTitle(indexEntity.getPageId().getPageTitle());
        searchData.setSnippet(getSnippet(indexEntity.getPageId().getPageContent(), query));
        searchData.setAbsRelevance(indexEntity.getRank());
        return searchData;
    }

    //Этот метод очень долго работате - на 89 страниц - 46 секунд
    private static String getSnippet(String content, String query) {
        List<String> words = LemmaFinder.getInstance().getWords(query);
        StringBuilder resultContent = new StringBuilder(content);

        for (String word : words) {
            Pattern pattern = Pattern.compile(word);
            Matcher matcher = pattern.matcher(resultContent);
            StringBuffer contentBuffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(contentBuffer, "<b>" + matcher.group() + "<b>");
            }
            matcher.appendTail(contentBuffer);
            resultContent = new StringBuilder(contentBuffer);
        }

        Pattern pattern = Pattern.compile(words.get(0));
        Matcher matcher = pattern.matcher(resultContent);
        if (matcher.find()) {
            int startWordIndex = matcher.start();

            return resultContent.substring(
                    Math.max(0, startWordIndex - 200),
                    Math.min(content.length(), startWordIndex + 200));
        }
        return null;
    }
}
