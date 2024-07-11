package searchengine.services.index;

import searchengine.dto.index.IndexResponse;

public interface IndexService {
    IndexResponse startIndexing();
    void test(String t);

}
