package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import searchengine.model.IndexEntity;

@Data
@NoArgsConstructor
public class SearchData {
    private Integer pageId;
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Integer absRelevance;
    private Double relevance;

    public static SearchData mapToSearchData(IndexEntity indexEntity) {
        return null;
    }
}
