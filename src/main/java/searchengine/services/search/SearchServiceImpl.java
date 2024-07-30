package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.contentparser.LemmaFinder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    public SearchResponse search(String query, String site, String offset, String limit) {
        Set<String> lemmas = LemmaFinder.getInstance().collectLemmas(query).keySet();

        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemmaIn(lemmas);
        if(lemmaEntityList.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }

        Collections.sort(lemmaEntityList, Comparator.comparing(LemmaEntity::getFrequency));
        List<IndexEntity> indexEntityList = indexRepository.findAllByLemmaId(lemmaEntityList.get(0).getId());

        Map<Integer, SearchData> searchDataMap = new HashMap<>();
        for (IndexEntity indexEntity : indexEntityList) {
            Optional<LemmaEntity> lemma = lemmaEntityList.stream()
                    .filter(l -> l.getId().compareTo(indexEntity.getLemmaId().getId()) > 0)
                    .findAny();
            if (lemma.isPresent()) {
                if(searchDataMap.containsKey(indexEntity.getPageId().getId())) {
                    SearchData searchData = searchDataMap.get(indexEntity.getPageId().getId());
                    searchData.setAbsRelevance(searchData.getAbsRelevance() + indexEntity.getRank());
                }
                SearchData searchData = new SearchData();
                searchData.setPageId(indexEntity.getPageId().getId());
                searchData.setSite(lemma.get().getSiteId().getUrl());
                searchData.setSiteName(lemma.get().getSiteId().getName());
                searchData.setUri(indexEntity.getPageId().getPath());
                searchData.setTitle(indexEntity.getPageId().getPageTitle());
                searchData.setSnippet(null);
                searchData.setAbsRelevance(indexEntity.getRank());
                searchDataMap.put(indexEntity.getPageId().getId(), searchData);
            }
        }

        if (searchDataMap.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }





        return null;
    }

    private IndexEntity countAbsRelevant(IndexEntity indexEntity) {
        indexRepository.countAbsRelevant(indexEntity.getId());
        return  indexEntity;
    }

}
