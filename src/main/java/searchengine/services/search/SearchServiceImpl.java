package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.contentparser.LemmaFinder;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        if (query.isBlank()) {
            return new SearchResponse(false, null, null, "Пустой запрос");
        }

        Set<String> lemmas = LemmaFinder.getInstance().collectLemmas(query).keySet();
        List<LemmaEntity> lemmaEntityList = new ArrayList<>();
        if (!site.equals("all")) {
            SiteEntity siteEntity = siteRepository.findByUrl(site);
            lemmaEntityList.addAll(lemmaRepository.findAllByLemmaInAndSiteId(lemmas, siteEntity));
        } else {
            lemmaEntityList.addAll(lemmaRepository.findAllByLemmaIn(lemmas));
        }

        if (lemmaEntityList.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }
        Collections.sort(lemmaEntityList, Comparator.comparing(LemmaEntity::getFrequency));

        List<IndexEntity> allIndexEntityList = indexRepository.findAllByLemmaId(lemmaEntityList.get(0).getId());
        List<IndexEntity> indexEntityList = allIndexEntityList.subList(
                Math.min(offset, allIndexEntityList.size()),
                Math.min(offset + limit, allIndexEntityList.size()));

        Map<Integer, SearchData> searchDataMap = new HashMap<>();
        for (IndexEntity indexEntity : indexEntityList) {
            Optional<LemmaEntity> lemma = lemmaEntityList.stream()
                    .filter(l -> l.getId().compareTo(indexEntity.getLemmaId().getId()) == 0)
                    .findAny();
            if (lemma.isPresent()) {
                if (searchDataMap.containsKey(indexEntity.getPageId().getId())) {
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

        SearchData maxAbsRel = searchDataMap.values()
                .stream().max(Comparator.comparing(SearchData::getAbsRelevance)).orElseThrow();

        for (SearchData searchData : searchDataMap.values()) {
            searchData.setRelevance(searchData.getAbsRelevance() / maxAbsRel.getAbsRelevance());
        }

        List<SearchData> searchDataList = new ArrayList<>(searchDataMap.values());
        searchDataList.sort(Comparator.comparing(SearchData::getRelevance).reversed());

        return new SearchResponse(true, allIndexEntityList.size(),
                searchDataList.subList(0, Math.min(limit, searchDataList.size())), null);
    }
}
