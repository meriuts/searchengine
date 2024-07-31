package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.contentparser.LemmaFinder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    public SearchResponse search(String query, String site, String offset, String limit) {
        if (query.isBlank()) {
            return new SearchResponse(false, null, null, "Пустой запрос");
        }

        Set<String> lemmas = LemmaFinder.getInstance().collectLemmas(query).keySet();
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemmaIn(lemmas);
        if(lemmaEntityList.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }

        Collections.sort(lemmaEntityList, Comparator.comparing(LemmaEntity::getFrequency));
        List<IndexEntity> indexEntityList = indexRepository.findAllByLemmaId(lemmaEntityList.get(0).getId());

        Map<Integer, SearchData> searchDataMap = new HashMap<>();
        for (IndexEntity indexEntity : indexEntityList) {
            //это можно же не делать - нужно проверять что список не пустой в объекте
            Optional<LemmaEntity> lemma = lemmaEntityList.stream()
                    .filter(l -> l.getId().compareTo(indexEntity.getLemmaId().getId()) == 0)
                    .findAny();
            if (lemma.isPresent()) {
                if(searchDataMap.containsKey(indexEntity.getPageId().getId())) {
                    SearchData searchData = searchDataMap.get(indexEntity.getPageId().getId());
                    searchData.setAbsRelevance(searchData.getAbsRelevance() + indexEntity.getRank());
                    continue;
                }
                SearchData searchData = SearchData.mapToSearchData(indexEntity, query);
                searchDataMap.put(indexEntity.getPageId().getId(), searchData);
            }
        }

        if (searchDataMap.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }

        List<SearchData> searchDataList = new ArrayList<>(searchDataMap.values());

        return new SearchResponse(true, searchDataList.size(), searchDataList, null);
    }
}
