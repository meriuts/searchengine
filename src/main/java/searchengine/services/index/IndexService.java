package searchengine.services.index;

import searchengine.dto.index.IndexRequest;
import searchengine.dto.index.IndexResponse;

public interface IndexService {
    IndexResponse startIndexing();
    IndexResponse stopIndexing();
    IndexResponse indexPage(IndexRequest url);

}
